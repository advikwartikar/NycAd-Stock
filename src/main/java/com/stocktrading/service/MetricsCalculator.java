package com.stocktrading.service;

import com.stocktrading.dto.TrendMetricsDTO;
import com.stocktrading.model.*;
import com.stocktrading.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetricsCalculator {

    @Autowired
    private ExperimentDecisionRepository decisionRepository;

    @Autowired
    private ExperimentStockRepository experimentStockRepository;

    public Map<MarketTrend, TrendMetricsDTO> calculateTrendMetrics(ExperimentSession session) {

        Map<MarketTrend, TrendMetricsDTO> result =
                new EnumMap<>(MarketTrend.class);

        for (MarketTrend trend : MarketTrend.values()) {
            result.put(trend, calculateForTrend(session, trend));
        }

        return result;
    }

    private TrendMetricsDTO calculateForTrend(ExperimentSession session,
                                              MarketTrend targetTrend) {

        TrendMetricsDTO metrics = new TrendMetricsDTO();

        // FIXED: Compare String with Enum using name()
        List<ExperimentStock> trendStocks =
                experimentStockRepository.findAll().stream()
                        .filter(s ->
                                s.getMarketTrend() != null &&
                                s.getMarketTrend().equalsIgnoreCase(targetTrend.name())
                        )
                        .collect(Collectors.toList());

        metrics.setStocksInTrend(trendStocks.size());

        if (trendStocks.isEmpty()) {
            return metrics;
        }

        List<ExperimentDecision> allDecisions =
                decisionRepository.findBySessionOrderByStockIndexAscDayNumberAsc(session);

        Map<Integer, List<ExperimentDecision>> decisionsByStock =
                allDecisions.stream()
                        .collect(Collectors.groupingBy(ExperimentDecision::getStockIndex));

        List<Double> returns = new ArrayList<>();
        int totalTrades = 0;
        int winningTrades = 0;
        double totalGross = 0.0;
        double totalLoss = 0.0;
        int totalDaysHeld = 0;

        for (ExperimentStock stock : trendStocks) {

            List<ExperimentDecision> stockDecisions =
                    decisionsByStock.get(stock.getSequenceOrder());

            if (stockDecisions == null || stockDecisions.isEmpty()) {
                continue;
            }

            double stockReturn = 0.0;
            int daysHeld = 0;

            for (ExperimentDecision decision : stockDecisions) {

                String action = decision.getAction();

                if ("BUY".equalsIgnoreCase(action)) {
                    totalTrades++;
                    daysHeld++;
                }

                else if ("SELL".equalsIgnoreCase(action)) {
                    totalTrades++;

                    double pl = decision.getPrice() * decision.getQuantity();
                    stockReturn += pl;

                    if (pl > 0) {
                        winningTrades++;
                        totalGross += pl;
                    } else {
                        totalLoss += Math.abs(pl);
                    }
                }
            }

            returns.add(stockReturn);
            totalDaysHeld += daysHeld;
        }

        double avgReturn = returns.isEmpty()
                ? 0.0
                : returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double volatility = calculateStdDev(returns);

        double sharpeRatio = volatility > 0
                ? (avgReturn / volatility)
                : 0.0;

        double winRate = totalTrades > 0
                ? (winningTrades * 100.0 / totalTrades)
                : 0.0;

        double profitFactor = totalLoss > 0
                ? (totalGross / totalLoss)
                : 0.0;

        double avgTimeInMarket = trendStocks.isEmpty()
                ? 0.0
                : (totalDaysHeld * 1.0 / trendStocks.size());

        metrics.setSharpeRatio(sharpeRatio);
        metrics.setMaxDrawdown(0.0);
        metrics.setVolatility(volatility);
        metrics.setWinRate(winRate);
        metrics.setNumberOfTrades(totalTrades);
        metrics.setProfitFactor(profitFactor);
        metrics.setAvgTimeInMarket(avgTimeInMarket);

        return metrics;
    }

    private double calculateStdDev(List<Double> values) {

        if (values.isEmpty()) return 0.0;

        double mean = values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }
}