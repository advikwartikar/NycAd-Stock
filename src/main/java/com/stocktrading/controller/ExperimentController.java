package com.stocktrading.controller;

import com.stocktrading.model.*;
import com.stocktrading.repository.ExperimentStockRepository;
import com.stocktrading.service.*;
import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.*;
import java.util.*;

@Controller
@RequestMapping("/experiment")
public class ExperimentController {

    @Autowired private ExperimentService experimentService;
    @Autowired private UserService userService;
    @Autowired private ExperimentStockRepository experimentStockRepository;

    @GetMapping("/start")
    public String startExperiment(Authentication auth, RedirectAttributes ra) {
        User user = userService.getUserByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        long stockCount = experimentStockRepository.count();
        if (stockCount < 10) {
            ra.addFlashAttribute("error",
                "Experiment setup incomplete – only " + stockCount + " stocks configured. " +
                "Please wait a moment and try again, or contact the administrator.");
            return "redirect:/dashboard";
        }

        try {
            experimentService.startExperiment(user);
            return "redirect:/experiment/trade";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not start experiment: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @GetMapping("/trade")
    public String tradingInterface(Model model, Authentication auth, RedirectAttributes ra) {
        User user = userService.getUserByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExperimentSession session = experimentService.getCurrentSession(user);

        if (session == null) {
            return "redirect:/experiment/start";
        }
        if (session.getCompleted()) {
            return "redirect:/experiment/summary";
        }
        if (experimentService.isSessionExpired(session)) {
            session.setCompleted(true);
            experimentService.saveSession(session);
            return "redirect:/experiment/summary";
        }

        ExperimentStock currentStock = experimentService.getCurrentStock(session);
        if (currentStock == null) {
            ra.addFlashAttribute("error",
                "Experiment stock not found (index " + session.getCurrentStockIndex() + "). " +
                "The experiment data may not have loaded yet – please wait 30 seconds and try again.");
            return "redirect:/dashboard";
        }

        Map<String, Object> state = experimentService.getCurrentState(session);

        List<Map<String, Object>> stockData = new ArrayList<>();
        Map<String, Object> currentDayData = null;
        try {
            stockData = loadStockData(currentStock, session.getCurrentDay());
            if (!stockData.isEmpty()) {
                currentDayData = stockData.get(stockData.size() - 1);
            }
        } catch (Exception e) {
            System.err.println("Error loading stock data: " + e.getMessage());
            e.printStackTrace();
        }

        model.addAttribute("stockData", stockData);
        model.addAttribute("currentDayData", currentDayData);
        model.addAttribute("state", state);
        model.addAttribute("session", session);
        model.addAttribute("stock", currentStock);
        model.addAttribute("user", user);

        return "experiment-trade";
    }

    @PostMapping("/decide")
    public String makeDecision(@RequestParam String action,
                               @RequestParam(required = false, defaultValue = "10") Integer quantity,
                               Authentication auth,
                               RedirectAttributes ra) {
        User user = userService.getUserByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExperimentSession session = experimentService.getCurrentSession(user);
        if (session == null || session.getCompleted()) {
            return "redirect:/experiment/summary";
        }

        boolean isLastDay   = session.getCurrentDay() == 9;
        boolean isLastStock = session.getCurrentStockIndex() == 9;

        try {
            ExperimentStock currentStock = experimentService.getCurrentStock(session);
            if (currentStock == null) {
                ra.addFlashAttribute("error", "Stock data not found.");
                return "redirect:/experiment/trade";
            }

            List<Map<String, Object>> stockData = loadStockData(currentStock, session.getCurrentDay());
            
            if (stockData.isEmpty()) {
                ra.addFlashAttribute("error", "No price data available for today.");
                return "redirect:/experiment/trade";
            }

            // Validate quantity for BUY/SELL
            if ("BUY".equalsIgnoreCase(action) || "SELL".equalsIgnoreCase(action)) {
                if (quantity == null || quantity <= 0) {
                    ra.addFlashAttribute("error", "Please enter a valid quantity.");
                    return "redirect:/experiment/trade";
                }
            } else {
                quantity = 0; // HOLD action
            }

            Double currentPrice = (Double) stockData.get(stockData.size() - 1).get("close");
            experimentService.makeDecision(session, action.toUpperCase(), currentPrice, quantity);

            if (isLastDay) {
                if (isLastStock) {
                    ra.addFlashAttribute("success", "Experiment completed!");
                    return "redirect:/experiment/summary";
                } else {
                    return "redirect:/experiment/episode-summary";
                }
            }

            return "redirect:/experiment/trade";

        } catch (Exception e) {
            System.err.println("ERROR in makeDecision: " + e.getMessage());
            e.printStackTrace();
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/experiment/trade";
        }
    }

    @GetMapping("/episode-summary")
    public String episodeSummary(Model model, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExperimentSession session = experimentService.getCurrentSession(user);
        if (session == null) return "redirect:/experiment/start";

        Integer completedStockIndex = session.getCurrentStockIndex() - 1;
        Map<String, Object> summary = experimentService.getEpisodeSummary(session, completedStockIndex);

        ExperimentStock completedStock = experimentStockRepository
                .findBySequenceOrder(completedStockIndex).orElse(null);

        model.addAttribute("summary", summary);
        model.addAttribute("stock", completedStock);
        model.addAttribute("session", session);
        model.addAttribute("user", user);
        model.addAttribute("nextStockIndex", session.getCurrentStockIndex() + 1);

        return "experiment-episode-summary";
    }

    @GetMapping("/summary")
    public String finalSummary(Model model, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExperimentSession session = experimentService.getAnySession(user);
        if (session == null) return "redirect:/dashboard";

        Map<String, Object> summary = experimentService.getSessionSummary(session);
        model.addAttribute("summary", summary);
        model.addAttribute("session", session);
        model.addAttribute("user", user);

        return "experiment-summary";
    }

    private List<Map<String, Object>> loadStockData(ExperimentStock stock, Integer currentDay) {
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            String csvPath = "data/" + stock.getStockSymbol().toLowerCase() + ".csv";
            InputStream is = getClass().getClassLoader().getResourceAsStream(csvPath);

            if (is == null) {
                System.err.println("ERROR: CSV not found: " + csvPath);
                return data;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String headerLine = br.readLine(); // Skip header
                
                List<String[]> allRows = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    allRows.add(line.split(","));
                }

                int startDay = stock.getSegmentStartDay();
                int endDay = startDay + currentDay;

                // Load data from startDay to startDay+currentDay
                for (int i = startDay; i <= endDay && i < allRows.size(); i++) {
                    String[] row = allRows.get(i);
                    if (row.length >= 5) {
                        Map<String, Object> dayData = new LinkedHashMap<>();
                        dayData.put("day", i - startDay + 1);
                        dayData.put("open", safeDouble(row[0]));
                        dayData.put("high", safeDouble(row[1]));
                        dayData.put("low", safeDouble(row[2]));
                        dayData.put("close", safeDouble(row[3]));
                        dayData.put("volume", safeLong(row[4]));
                        if (row.length >= 6) dayData.put("sma", safeDouble(row[5]));
                        if (row.length >= 7) dayData.put("rsi", safeDouble(row[6]));
                        data.add(dayData);
                    }
                }

            }
        } catch (Exception e) {
            System.err.println("ERROR loading stock data: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    private Double safeDouble(String s) {
        if (s == null || s.trim().isEmpty()) return 0.0;
        try { 
            return Double.parseDouble(s.trim()); 
        } catch (Exception e) { 
            return 0.0; 
        }
    }

    private Long safeLong(String s) {
        if (s == null || s.trim().isEmpty()) return 0L;
        try { 
            return Long.parseLong(s.trim()); 
        } catch (Exception e) {
            try { 
                return (long) Double.parseDouble(s.trim()); 
            } catch (Exception ex) { 
                return 0L; 
            }
        }
    }
}
