package com.stocktrading.config;

import com.stocktrading.model.ExperimentStock;
import com.stocktrading.repository.ExperimentStockRepository;
import com.stocktrading.service.VolatilityAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Order(2)
public class ExperimentDataLoader implements CommandLineRunner {

    @Autowired private VolatilityAnalyzer volatilityAnalyzer;
    @Autowired private ExperimentStockRepository experimentStockRepository;

    // Fixed fallback windows guaranteed to have data
    private static final int[][] FALLBACK_WINDOWS = {
        {10,19},{30,39},{50,59},{70,79},{90,99},
        {20,29},{40,49},{60,69},{80,89},{100,109}
    };

    @Override
    public void run(String... args) {
        if (experimentStockRepository.count() >= 10) {
            System.out.println("✓ Experiment stocks ready: " + experimentStockRepository.count());
            return;
        }

        System.out.println("========================================");
        System.out.println("SETTING UP EXPERIMENT STOCKS");
        System.out.println("========================================");

        boolean success = false;

        // Try volatility analysis first
        try {
            List<VolatilityAnalyzer.StockVolatility> top10 = volatilityAnalyzer.analyzeTop10Stocks();
            if (top10 != null && top10.size() >= 10) {
                experimentStockRepository.deleteAll();
                for (int i = 0; i < 10; i++) {
                    VolatilityAnalyzer.StockVolatility sv = top10.get(i);
                    if (!sv.windows.isEmpty()) {
                        VolatilityAnalyzer.VolatileWindow w = sv.windows.get(0);
                        ExperimentStock es = new ExperimentStock();
                        es.setSequenceOrder(i);
                        es.setStockSymbol(sv.symbol);
                        es.setSegmentStartDay(w.startDay);
                        es.setSegmentEndDay(w.endDay);
                        es.setCsvFilePath("data/" + sv.symbol.toLowerCase() + ".csv");
                        experimentStockRepository.save(es);
                        System.out.printf("  [%d] %s days %d-%d (vol=%.4f)%n", i+1, sv.symbol, w.startDay, w.endDay, w.volatility);
                    }
                }
                success = experimentStockRepository.count() >= 10;
            }
        } catch (Exception e) {
            System.err.println("Volatility analysis failed: " + e.getMessage() + " — using fallback");
        }

        // Fallback: always use stocks 1-10 with fixed windows
        if (!success) {
            experimentStockRepository.deleteAll();
            System.out.println("Using fallback stock selection (stocks 1–10)");
            for (int i = 0; i < 10; i++) {
                ExperimentStock es = new ExperimentStock();
                es.setSequenceOrder(i);
                es.setStockSymbol("STOCK_" + (i + 1));
                es.setSegmentStartDay(FALLBACK_WINDOWS[i][0]);
                es.setSegmentEndDay(FALLBACK_WINDOWS[i][1]);
                es.setCsvFilePath("data/stock_" + (i + 1) + ".csv");
                experimentStockRepository.save(es);
                System.out.printf("  Fallback [%d] STOCK_%d days %d-%d%n", i+1, i+1, FALLBACK_WINDOWS[i][0], FALLBACK_WINDOWS[i][1]);
            }
        }

        System.out.println("✓ Experiment ready with " + experimentStockRepository.count() + " stocks");
        System.out.println("========================================");
    }
}
