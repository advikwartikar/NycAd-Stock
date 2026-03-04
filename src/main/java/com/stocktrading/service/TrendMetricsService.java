package com.stocktrading.service;

import com.stocktrading.model.*;
import com.stocktrading.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrendMetricsService {

    @Autowired private ExperimentDecisionRepository decisionRepository;
    @Autowired private ExperimentStockRepository experimentStockRepository;

    public Map<String, Map<String, Object>> calculateMetricsByTrend(ExperimentSession session) {
        Map<String, Map<String, Object>> trendMetrics = new HashMap<>();
        
        trendMetrics.put("Bullish", new HashMap<>());
        trendMetrics.put("Bearish", new HashMap<>());
        trendMetrics.put("Sideways", new HashMap<>());
        
        List<ExperimentDecision> allDecisions = decisionRepository
            .findBySessionOrderByStockIndexAscDayNumberAsc(session);
        
        if (allDecisions == null) allDecisions = new ArrayList<>();
        
        Map<Integer, List<ExperimentDecision>> decisionsByStock = allDecisions.stream()
            .collect(Collectors.groupingBy(ExperimentDecision::getStockIndex));
        
        for (String trend : Arrays.asList("Bullish", "Bearish", "Sideways")) {
            Map<String, Object> metrics = calculateTrendMetrics(session, decisionsByStock, trend);
            trendMetrics.put(trend, metrics);
        }
        
        return trendMetrics;
    }
    
    private Map<String, Object> calculateTrendMetrics(
            ExperimentSession session,
            Map<Integer, List<ExperimentDecision>> decisionsByStock,
            String targetTrend) {
        
        Map<String, Object> metrics = new HashMap<>();
        
        List<ExperimentStock> trendStocks = experimentStockRepository.findAll().stream()
            .filter(s -> targetTrend.equals(s.getMarketTrend()))
            .collect(Collectors.toList());
        
        List<Double> returns = new ArrayList<>();
        int totalTrades = 0;
        int winningTrades = 0;
        int totalDaysInMarket = 0;
        double totalGross = 0.0;
        double totalLoss = 0.0;
        double maxDrawdown = 0.0;
        
        for (ExperimentStock stock : trendStocks) {
            List<ExperimentDecision> stockDecisions = decisionsByStock.get(stock.getSequenceOrder());
            
            if (stockDecisions == null || stockDecisions.isEmpty()) {
                continue;
            }
            
            int daysHeld = 0;
            double stockPL = 0.0;
            
            for (ExperimentDecision decision : stockDecisions) {
                if ("BUY".equals(decision.getAction())) {
                    totalTrades++;
                    daysHeld++;
                } else if ("SELL".equals(decision.getAction())) {
                    totalTrades++;
                    double pl = decision.getPrice() * decision.getQuantity();
                    stockPL += pl;
                    if (pl > 0) {
                        winningTrades++;
                        totalGross += pl;
                    } else {
                        totalLoss += Math.abs(pl);
                    }
                }
            }
            
            totalDaysInMarket += daysHeld;
            returns.add(stockPL);
        }
        
        double avgReturn = returns.isEmpty() ? 0.0 : 
            returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double volatility = returns.isEmpty() ? 0.0 : calculateStdDev(returns);
        double sharpeRatio = volatility > 0 ? (avgReturn / volatility) : 0.0;
        double winRate = totalTrades > 0 ? (winningTrades * 100.0 / totalTrades) : 0.0;
        double profitFactor = totalLoss > 0 ? (totalGross / totalLoss) : 0.0;
        double avgTimeInMarket = trendStocks.isEmpty() ? 0.0 : (totalDaysInMarket * 1.0 / trendStocks.size());
        
        metrics.put("avgReturn", avgReturn);
        metrics.put("sharpeRatio", sharpeRatio);
        metrics.put("maxDrawdown", maxDrawdown);
        metrics.put("volatility", volatility);
        metrics.put("winRate", winRate);
        metrics.put("numberOfTrades", totalTrades);
        metrics.put("profitFactor", profitFactor);
        metrics.put("avgTimeInMarket", avgTimeInMarket);
        metrics.put("stocksInTrend", trendStocks.size());
        
        return metrics;
    }
    
    private double calculateStdDev(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }
}
