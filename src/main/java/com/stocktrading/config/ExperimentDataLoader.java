package com.stocktrading.config;

import com.stocktrading.model.ExperimentStock;
import com.stocktrading.repository.ExperimentStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Order(2)
public class ExperimentDataLoader implements CommandLineRunner {

    @Autowired
    private ExperimentStockRepository experimentStockRepository;

    private static final Map<String, String> STOCK_TREND_MAP = Map.ofEntries(
        Map.entry("stock_1", "Bullish"),
        Map.entry("stock_2", "Bullish"),
        Map.entry("stock_3", "Bullish"),
        Map.entry("stock_4", "Bullish"),
        Map.entry("stock_5", "Bullish"),
        Map.entry("stock_6", "Bearish"),
        Map.entry("stock_7", "Bearish"),
        Map.entry("stock_8", "Bearish"),
        Map.entry("stock_9", "Bearish"),
        Map.entry("stock_10", "Bearish"),
        Map.entry("stock_11", "Sideways"),
        Map.entry("stock_12", "Sideways"),
        Map.entry("stock_13", "Sideways"),
        Map.entry("stock_14", "Sideways"),
        Map.entry("stock_15", "Sideways")
    );

    @Override
    public void run(String... args) {
        if (experimentStockRepository.count() >= 15) {
            System.out.println("✓ Experiment stocks ready: " + experimentStockRepository.count());
            return;
        }

        System.out.println("========================================");
        System.out.println("SETTING UP 15 EXPERIMENT STOCKS");
        System.out.println("========================================");

        experimentStockRepository.deleteAll();

        for (int i = 1; i <= 15; i++) {
            String stockSymbol = "stock_" + i;
            String marketTrend = STOCK_TREND_MAP.get(stockSymbol);
            
            ExperimentStock expStock = new ExperimentStock();
            expStock.setStockSymbol(stockSymbol);
            expStock.setSequenceOrder(i - 1);
            expStock.setSegmentStartDay(0);
            expStock.setSegmentEndDay(99);
            expStock.setCsvFilePath("data/" + stockSymbol + ".csv");
            expStock.setMarketTrend(marketTrend);
            
            experimentStockRepository.save(expStock);
            
            System.out.println("  ✓ Stock " + i + ": " + stockSymbol + " (" + marketTrend + ")");
        }

        System.out.println("✓ Loaded 15 experiment stocks:");
        System.out.println("  - 5 Bullish (stocks 1-5)");
        System.out.println("  - 5 Bearish (stocks 6-10)");
        System.out.println("  - 5 Sideways (stocks 11-15)");
        System.out.println("========================================");
    }
}
