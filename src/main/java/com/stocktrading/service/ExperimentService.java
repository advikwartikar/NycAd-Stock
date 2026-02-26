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
    public static final Integer TOTAL_STOCKS = 10;
    public static final Integer DAYS_PER_STOCK = 10;
    public static final long TIME_LIMIT_MINUTES = 120; // 2 hours

    // ── Session management ─────────────────────────────────────────────────

    public ExperimentSession startExperiment(User user) {
        Optional<ExperimentSession> existing = sessionRepository.findByUserAndCompletedFalse(user);
        if (existing.isPresent()) return existing.get();
        ExperimentSession s = new ExperimentSession(user);
        return sessionRepository.save(s);
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

    public ExperimentSession saveSession(ExperimentSession s) {
        return sessionRepository.save(s);
    }

    // ── State helpers ──────────────────────────────────────────────────────

    public boolean isSessionExpired(ExperimentSession s) {
        if (s.getStartTime() == null) return false;
        return Duration.between(s.getStartTime(), LocalDateTime.now()).toMinutes() >= TIME_LIMIT_MINUTES;
    }

    public ExperimentStock getCurrentStock(ExperimentSession s) {
        return experimentStockRepository.findBySequenceOrder(s.getCurrentStockIndex()).orElse(null);
    }

    public Map<String, Object> getCurrentState(ExperimentSession s) {
        Map<String, Object> st = new HashMap<>();
        ExperimentStock cs = getCurrentStock(s);
        st.put("stockIndex",     s.getCurrentStockIndex());
        st.put("dayNumber",      s.getCurrentDay());
        st.put("totalStocks",    TOTAL_STOCKS);
        st.put("daysPerStock",   DAYS_PER_STOCK);
        st.put("currentCapital", s.getCurrentCapital());
        st.put("currentShares",  s.getCurrentShares());
        st.put("stockSymbol",    cs != null ? cs.getStockSymbol() : "?");
        int done = s.getCurrentStockIndex() * DAYS_PER_STOCK + s.getCurrentDay();
        st.put("progressPercent", (done * 100.0) / (TOTAL_STOCKS * DAYS_PER_STOCK));
        if (s.getStartTime() != null) {
            long elapsed = Duration.between(s.getStartTime(), LocalDateTime.now()).toMinutes();
            st.put("timeRemainingMinutes", Math.max(0, TIME_LIMIT_MINUTES - elapsed));
        } else {
            st.put("timeRemainingMinutes", TIME_LIMIT_MINUTES);
        }
        return st;
    }

    // ── Decision with VARIABLE QUANTITY ────────────────────────────────────

    public ExperimentDecision makeDecision(ExperimentSession session, String action, Double price, Integer quantity) {
        if (isSessionExpired(session)) {
            session.setCompleted(true);
            session.setEndTime(LocalDateTime.now());
            sessionRepository.save(session);
            throw new RuntimeException("Time limit reached");
        }

        // Validate quantity
        if (quantity == null || quantity < 0) {
            quantity = 0;
        }

        ExperimentDecision d = new ExperimentDecision();
        d.setSession(session);
        d.setStockIndex(session.getCurrentStockIndex());
        d.setDayNumber(session.getCurrentDay());
        d.setAction(action);
        d.setPrice(price);
        d.setQuantity(quantity);

        int shares = session.getCurrentShares();
        double capital = session.getCurrentCapital();

        switch (action.toUpperCase()) {
            case "BUY":
                double cost = price * quantity;
                if (capital < cost) {
                    throw new RuntimeException("Insufficient capital. Need ₹" + 
                        String.format("%.2f", cost) + " but only have ₹" + 
                        String.format("%.2f", capital));
                }
                capital -= cost;
                shares += quantity;
                break;
            case "SELL":
                if (shares < quantity) {
                    throw new RuntimeException("Insufficient shares. Need " + quantity + 
                        " shares but only have " + shares);
                }
                capital += price * quantity;
                shares -= quantity;
                break;
            case "HOLD":
                // No changes
                break;
            default:
                throw new RuntimeException("Invalid action: " + action);
        }

        session.setCurrentCapital(capital);
        session.setCurrentShares(shares);

        // Advance day
        session.setCurrentDay(session.getCurrentDay() + 1);

        // End of stock episode (completed 10 days)
        if (session.getCurrentDay() >= DAYS_PER_STOCK) {
            // Auto-sell any remaining shares at current price
            if (shares > 0) {
                capital += shares * price;
                session.setCurrentCapital(capital);
                session.setCurrentShares(0);
            }
            
            // Move to next stock
            session.setCurrentStockIndex(session.getCurrentStockIndex() + 1);
            session.setCurrentDay(0);

            // Check if all 10 stocks completed
            if (session.getCurrentStockIndex() >= TOTAL_STOCKS) {
                session.setCompleted(true);
                session.setEndTime(LocalDateTime.now());
            } else {
                // IMPORTANT: Capital carries over! Only reset shares
                session.setCurrentShares(0);
                // DO NOT reset capital - it accumulates!
            }
        }

        sessionRepository.save(session);
        decisionRepository.save(d);
        return d;
    }

    // ── Summaries ──────────────────────────────────────────────────────────

    public Map<String, Object> getEpisodeSummary(ExperimentSession session, Integer stockIndex) {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            List<ExperimentDecision> decisions = decisionRepository
                .findBySessionAndStockIndexOrderByDayNumberAsc(session, stockIndex);
            
            if (decisions == null) {
                decisions = new ArrayList<>();
            }
            
            long buys = 0;
            long sells = 0;
            long holds = 0;
            
            for (ExperimentDecision d : decisions) {
                if ("BUY".equals(d.getAction())) buys++;
                else if ("SELL".equals(d.getAction())) sells++;
                else if ("HOLD".equals(d.getAction())) holds++;
            }
            
            double startCapital = 100000.0;
            double endCapital = session.getCurrentCapital() != null ? session.getCurrentCapital() : 100000.0;
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
            e.printStackTrace();
            summary.put("totalDecisions", 0);
            summary.put("buys", 0);
            summary.put("sells", 0);
            summary.put("holds", 0);
            summary.put("startCapital", 100000.0);
            summary.put("endCapital", 100000.0);
            summary.put("profitLoss", 0.0);
            summary.put("decisions", new ArrayList<>());
        }
        
        return summary;
    }

    public Map<String, Object> getSessionSummary(ExperimentSession session) {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            List<ExperimentDecision> allDecisions = decisionRepository
                .findBySessionOrderByStockIndexAscDayNumberAsc(session);
            
            if (allDecisions == null) {
                allDecisions = new ArrayList<>();
            }
            
            long totalBuys = 0;
            long totalSells = 0;
            long totalHolds = 0;
            
            for (ExperimentDecision d : allDecisions) {
                if ("BUY".equals(d.getAction())) totalBuys++;
                else if ("SELL".equals(d.getAction())) totalSells++;
                else if ("HOLD".equals(d.getAction())) totalHolds++;
            }
            
            double startCapital = 100000.0;
            double finalCapital = session.getCurrentCapital() != null ? session.getCurrentCapital() : 100000.0;
            double totalProfitLoss = finalCapital - startCapital;
            
            Map<Integer, List<ExperimentDecision>> decisionsByStock = new HashMap<>();
            for (ExperimentDecision d : allDecisions) {
                Integer stockIdx = d.getStockIndex();
                if (!decisionsByStock.containsKey(stockIdx)) {
                    decisionsByStock.put(stockIdx, new ArrayList<>());
                }
                decisionsByStock.get(stockIdx).add(d);
            }
            
            summary.put("totalDecisions", allDecisions.size());
            summary.put("totalBuys", totalBuys);
            summary.put("totalSells", totalSells);
            summary.put("totalHolds", totalHolds);
            summary.put("startCapital", startCapital);
            summary.put("finalCapital", finalCapital);
            summary.put("totalProfitLoss", totalProfitLoss);
            summary.put("stocksTraded", session.getCurrentStockIndex() != null ? session.getCurrentStockIndex() : 0);
            summary.put("decisionsByStock", decisionsByStock);
            summary.put("session", session);
            
        } catch (Exception e) {
            System.err.println("ERROR in getSessionSummary: " + e.getMessage());
            e.printStackTrace();
            summary.put("totalDecisions", 0);
            summary.put("totalBuys", 0);
            summary.put("totalSells", 0);
            summary.put("totalHolds", 0);
            summary.put("startCapital", 100000.0);
            summary.put("finalCapital", 100000.0);
            summary.put("totalProfitLoss", 0.0);
            summary.put("stocksTraded", 0);
            summary.put("decisionsByStock", new HashMap<>());
            summary.put("session", session);
        }
        
        return summary;
    }
}
