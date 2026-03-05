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
        Map<MarketTrend, TrendMetricsDTO> result = new EnumMap<>(MarketTrend.class);

        List<ExperimentDecision> allDecisions = decisionRepository
                .findBySessionOrderByStockIndexAscDayNumberAsc(session);
        Map<Integer, List<ExperimentDecision>> decisionsByStock = allDecisions.stream()
                .collect(Collectors.groupingBy(ExperimentDecision::getStockIndex, TreeMap::new, Collectors.toList()));

        List<ExperimentStock> allStocks = experimentStockRepository.findAllByOrderBySequenceOrderAsc();
        if (allStocks.isEmpty()) {
            for (MarketTrend trend : MarketTrend.values()) {
                result.put(trend, new TrendMetricsDTO());
            }
            return result;
        }

        double runningCapital = ExperimentService.INITIAL_CAPITAL;
        Map<Integer, StockMetrics> stockMetricsByIndex = new HashMap<>();

        for (ExperimentStock stock : allStocks) {
            List<ExperimentDecision> stockDecisions = decisionsByStock
                    .getOrDefault(stock.getSequenceOrder(), Collections.emptyList());

            StockMetrics stockMetrics = calculateStockMetrics(stockDecisions, runningCapital);
            stockMetricsByIndex.put(stock.getSequenceOrder(), stockMetrics);
            runningCapital = stockMetrics.finalCapital;
        }

        for (MarketTrend trend : MarketTrend.values()) {
            result.put(trend, aggregateForTrend(allStocks, stockMetricsByIndex, trend));
        }

        return result;
    }

    private TrendMetricsDTO aggregateForTrend(List<ExperimentStock> allStocks,
                                              Map<Integer, StockMetrics> stockMetricsByIndex,
                                              MarketTrend trend) {
        TrendMetricsDTO metrics = new TrendMetricsDTO();

        List<ExperimentStock> trendStocks = allStocks.stream()
                .filter(s -> matchesTrend(s.getMarketTrend(), trend))
                .toList();

        metrics.setStocksInTrend(trendStocks.size());
        if (trendStocks.isEmpty()) {
            return metrics;
        }

        List<Double> stockReturns = new ArrayList<>();
        List<Double> dailyReturns = new ArrayList<>();
        double grossProfit = 0.0;
        double grossLoss = 0.0;
        int winningTrades = 0;
        int totalSellTrades = 0;
        int totalTrades = 0;
        int totalDaysInMarket = 0;
        double maxDrawdown = 0.0;

        for (ExperimentStock stock : trendStocks) {
            StockMetrics sm = stockMetricsByIndex.get(stock.getSequenceOrder());
            if (sm == null) {
                continue;
            }

            stockReturns.add(sm.returnPercent);
            dailyReturns.addAll(sm.dailyReturns);
            totalTrades += sm.tradeCount;
            totalSellTrades += sm.sellTradeCount;
            winningTrades += sm.winningSellTrades;
            totalDaysInMarket += sm.daysInMarket;
            grossProfit += sm.grossProfit;
            grossLoss += sm.grossLoss;
            maxDrawdown = Math.max(maxDrawdown, sm.maxDrawdown);
        }

        double avgReturn = stockReturns.isEmpty()
                ? 0.0
                : stockReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double volatility = calculateStdDev(dailyReturns);
        double sharpeRatio = volatility > 0 ? (avgReturn / volatility) : 0.0;
        double winRate = totalSellTrades > 0 ? (winningTrades * 100.0 / totalSellTrades) : 0.0;
        double profitFactor = grossLoss > 0 ? (grossProfit / grossLoss) : (grossProfit > 0 ? grossProfit : 0.0);
        double avgTimeInMarket = trendStocks.isEmpty() ? 0.0 : (totalDaysInMarket * 1.0 / trendStocks.size());

        metrics.setSharpeRatio(sharpeRatio);
        metrics.setMaxDrawdown(maxDrawdown);
        metrics.setVolatility(volatility);
        metrics.setWinRate(winRate);
        metrics.setNumberOfTrades(totalTrades);
        metrics.setProfitFactor(profitFactor);
        metrics.setAvgTimeInMarket(avgTimeInMarket);

        return metrics;
    }

    private StockMetrics calculateStockMetrics(List<ExperimentDecision> stockDecisions, double initialCapital) {
        StockMetrics metrics = new StockMetrics();
        metrics.finalCapital = initialCapital;

        if (stockDecisions == null || stockDecisions.isEmpty()) {
            return metrics;
        }

        stockDecisions = stockDecisions.stream()
                .sorted(Comparator.comparing(ExperimentDecision::getDayNumber))
                .toList();

        double cash = initialCapital;
        int shares = 0;
        double avgCost = 0.0;

        double peakEquity = initialCapital;
        Double previousEquity = null;
        double lastPrice = stockDecisions.get(0).getPrice() != null ? stockDecisions.get(0).getPrice() : 0.0;

        for (ExperimentDecision decision : stockDecisions) {
            String action = decision.getAction() == null ? "HOLD" : decision.getAction().toUpperCase();
            int quantity = decision.getQuantity() == null ? 0 : decision.getQuantity();
            double price = decision.getPrice() == null ? 0.0 : decision.getPrice();
            lastPrice = price;

            if ("BUY".equals(action) && quantity > 0) {
                double cost = price * quantity;
                if (shares + quantity > 0) {
                    avgCost = ((avgCost * shares) + cost) / (shares + quantity);
                }
                cash -= cost;
                shares += quantity;
                metrics.tradeCount++;
            } else if ("SELL".equals(action) && quantity > 0) {
                int sellQty = Math.min(quantity, shares);
                if (sellQty > 0) {
                    double pnl = (price - avgCost) * sellQty;
                    if (pnl > 0) {
                        metrics.grossProfit += pnl;
                        metrics.winningSellTrades++;
                    } else if (pnl < 0) {
                        metrics.grossLoss += Math.abs(pnl);
                    }
                    metrics.sellTradeCount++;
                    cash += price * sellQty;
                    shares -= sellQty;
                    if (shares == 0) {
                        avgCost = 0.0;
                    }
                }
                metrics.tradeCount++;
            }

            if (shares > 0) {
                metrics.daysInMarket++;
            }

            double equity = cash + (shares * price);
            if (previousEquity != null && previousEquity != 0) {
                metrics.dailyReturns.add(((equity - previousEquity) / previousEquity) * 100.0);
            }
            previousEquity = equity;

            peakEquity = Math.max(peakEquity, equity);
            if (peakEquity > 0) {
                double drawdown = ((peakEquity - equity) / peakEquity) * 100.0;
                metrics.maxDrawdown = Math.max(metrics.maxDrawdown, drawdown);
            }
        }

        if (shares > 0) {
            double pnl = (lastPrice - avgCost) * shares;
            if (pnl > 0) {
                metrics.grossProfit += pnl;
                metrics.winningSellTrades++;
            } else if (pnl < 0) {
                metrics.grossLoss += Math.abs(pnl);
            }
            metrics.sellTradeCount++;
            cash += lastPrice * shares;
        }

        metrics.finalCapital = cash;
        metrics.returnPercent = initialCapital == 0
                ? 0.0
                : ((metrics.finalCapital - initialCapital) / initialCapital) * 100.0;

        return metrics;
    }

    private boolean matchesTrend(String stockTrendValue, MarketTrend trend) {
        if (stockTrendValue == null) {
            return false;
        }
        return stockTrendValue.equalsIgnoreCase(trend.name())
                || stockTrendValue.equalsIgnoreCase(trend.getDisplayName());
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

    private static class StockMetrics {
        double finalCapital = 0.0;
        double returnPercent = 0.0;
        double grossProfit = 0.0;
        double grossLoss = 0.0;
        int tradeCount = 0;
        int sellTradeCount = 0;
        int winningSellTrades = 0;
        int daysInMarket = 0;
        double maxDrawdown = 0.0;
        List<Double> dailyReturns = new ArrayList<>();
    }
}
