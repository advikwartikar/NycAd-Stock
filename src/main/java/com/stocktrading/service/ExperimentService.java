package com.stocktrading.service;

import com.stocktrading.model.*;
import com.stocktrading.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ExperimentService {

    @Autowired private ExperimentSessionRepository sessionRepository;
    @Autowired private ExperimentDecisionRepository decisionRepository;
    @Autowired private ExperimentStockRepository experimentStockRepository;

    public static final Double INITIAL_CAPITAL = 100000.0;
    public static final Integer TOTAL_STOCKS = 15;
    public static final Integer DAYS_PER_STOCK = 10;
    public static final long TIME_LIMIT_MINUTES = 120;

    public ExperimentSession startExperiment(User user) {
        Optional<ExperimentSession> existing = sessionRepository.findByUserAndCompletedFalse(user);
        if (existing.isPresent()) return existing.get();
        
        ExperimentSession session = new ExperimentSession(user);
        return sessionRepository.save(session);
    }

    public ExperimentSession getCurrentSession(User user) {
        return sessionRepository.findByUserAndCompletedFalse(user).orElse(null);
    }

    public ExperimentSession getAnySession(User user) {
        Optional<ExperimentSession> active = sessionRepository.findByUserAndCompletedFalse(user);
        if (active.isPresent()) return active.get();
        
        List<ExperimentSession> all = sessionRepository.findByUser(user);
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    public ExperimentSession saveSession(ExperimentSession session) {
        return sessionRepository.save(session);
    }

    public boolean isSessionExpired(ExperimentSession session) {
        if (session.getStartTime() == null) return false;
        return Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes() >= TIME_LIMIT_MINUTES;
    }

    public ExperimentStock getCurrentStock(ExperimentSession session) {
        return experimentStockRepository.findBySequenceOrder(session.getCurrentStockIndex()).orElse(null);
    }

    public Map<String, Object> getCurrentState(ExperimentSession session) {
        Map<String, Object> state = new HashMap<>();
        ExperimentStock currentStock = getCurrentStock(session);
        
        state.put("stockIndex", session.getCurrentStockIndex());
        state.put("dayNumber", session.getCurrentDay());
        state.put("totalStocks", TOTAL_STOCKS);
        state.put("daysPerStock", DAYS_PER_STOCK);
        state.put("currentCapital", session.getCurrentCapital());
        state.put("currentShares", session.getCurrentShares());
        state.put("stockSymbol", currentStock != null ? currentStock.getStockSymbol() : "Unknown");
       state.put("marketTrend", currentStock != null
        ? currentStock.getMarketTrend()
        : "Unknown");
        int done = session.getCurrentStockIndex() * DAYS_PER_STOCK + session.getCurrentDay();
        state.put("progressPercent", (done * 100.0) / (TOTAL_STOCKS * DAYS_PER_STOCK));
        
        if (session.getStartTime() != null) {
            long elapsed = Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
            state.put("timeRemainingMinutes", Math.max(0, TIME_LIMIT_MINUTES - elapsed));
        } else {
            state.put("timeRemainingMinutes", TIME_LIMIT_MINUTES);
        }
        
        return state;
    }

    public ExperimentDecision makeDecision(ExperimentSession session, String action, Double price, Integer quantity) {
        if (isSessionExpired(session)) {
            session.setCompleted(true);
            session.setEndTime(LocalDateTime.now());
            sessionRepository.save(session);
            throw new RuntimeException("Time limit reached");
        }

        if (quantity == null || quantity < 0) {
            quantity = 0;
        }

        ExperimentDecision decision = new ExperimentDecision();
        decision.setSession(session);
        decision.setStockIndex(session.getCurrentStockIndex());
        decision.setDayNumber(session.getCurrentDay());
        decision.setAction(action);
        decision.setPrice(price);
        decision.setQuantity(quantity);

        int shares = session.getCurrentShares();
        double capital = session.getCurrentCapital();

        switch (action.toUpperCase()) {
            case "BUY":
                double cost = price * quantity;
                if (capital < cost) {
                    throw new RuntimeException("Insufficient capital");
                }
                capital -= cost;
                shares += quantity;
                break;
            case "SELL":
                if (shares < quantity) {
                    throw new RuntimeException("Insufficient shares");
                }
                capital += price * quantity;
                shares -= quantity;
                break;
            case "HOLD":
                break;
            default:
                throw new RuntimeException("Invalid action");
        }

        session.setCurrentCapital(capital);
        session.setCurrentShares(shares);
        session.setCurrentDay(session.getCurrentDay() + 1);

        if (session.getCurrentDay() >= DAYS_PER_STOCK) {
            if (shares > 0) {
                capital += shares * price;
                session.setCurrentCapital(capital);
                session.setCurrentShares(0);
            }
            
            session.setCurrentStockIndex(session.getCurrentStockIndex() + 1);
            session.setCurrentDay(0);

            if (session.getCurrentStockIndex() >= TOTAL_STOCKS) {
                session.setCompleted(true);
                session.setEndTime(LocalDateTime.now());
            } else {
                session.setCurrentShares(0);
            }
        }

        sessionRepository.save(session);
        decisionRepository.save(decision);
        return decision;
    }

    public Map<String, Object> getEpisodeSummary(ExperimentSession session, Integer stockIndex) {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            List<ExperimentDecision> decisions = decisionRepository
                .findBySessionAndStockIndexOrderByDayNumberAsc(session, stockIndex);
            
            if (decisions == null) decisions = new ArrayList<>();
            
            long buys = decisions.stream().filter(d -> "BUY".equals(d.getAction())).count();
            long sells = decisions.stream().filter(d -> "SELL".equals(d.getAction())).count();
            long holds = decisions.stream().filter(d -> "HOLD".equals(d.getAction())).count();
            
            double startCapital = INITIAL_CAPITAL;
            double endCapital = session.getCurrentCapital() != null ? session.getCurrentCapital() : INITIAL_CAPITAL;
            double profitLoss = endCapital - startCapital;
            
            summary.put("totalDecisions", decisions.size());
            summary.put("buys", buys);
            summary.put("sells", sells);
            summary.put("holds", holds);
            summary.put("startCapital", startCapital);
            summary.put("endCapital", endCapital);
            summary.put("profitLoss", profitLoss);
            summary.put("decisions", decisions);
            
        } catch (Exception e) {
            System.err.println("ERROR in getEpisodeSummary: " + e.getMessage());
            summary.put("totalDecisions", 0);
            summary.put("decisions", new ArrayList<>());
        }
        
        return summary;
    }

    public Map<String, Object> getSessionSummary(ExperimentSession session) {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            List<ExperimentDecision> allDecisions = decisionRepository
                .findBySessionOrderByStockIndexAscDayNumberAsc(session);
            
            if (allDecisions == null) allDecisions = new ArrayList<>();
            
            long totalBuys = allDecisions.stream().filter(d -> "BUY".equals(d.getAction())).count();
            long totalSells = allDecisions.stream().filter(d -> "SELL".equals(d.getAction())).count();
            long totalHolds = allDecisions.stream().filter(d -> "HOLD".equals(d.getAction())).count();
            
            double startCapital = INITIAL_CAPITAL;
            double finalCapital = session.getCurrentCapital() != null ? session.getCurrentCapital() : INITIAL_CAPITAL;
            double totalProfitLoss = finalCapital - startCapital;
            
            summary.put("totalDecisions", allDecisions.size());
            summary.put("totalBuys", totalBuys);
            summary.put("totalSells", totalSells);
            summary.put("totalHolds", totalHolds);
            summary.put("startCapital", startCapital);
            summary.put("finalCapital", finalCapital);
            summary.put("totalProfitLoss", totalProfitLoss);
            summary.put("stocksTraded", session.getCurrentStockIndex() != null ? session.getCurrentStockIndex() : 0);
            summary.put("session", session);
            
        } catch (Exception e) {
            System.err.println("ERROR in getSessionSummary: " + e.getMessage());
            summary.put("session", session);
        }
        
        return summary;
    }
}
