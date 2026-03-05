package com.stocktrading.controller;

import com.stocktrading.model.User;
import com.stocktrading.model.ExperimentSession;
import com.stocktrading.model.ExperimentDecision;
import com.stocktrading.model.ExperimentStock;
import com.stocktrading.model.MarketTrend;
import com.stocktrading.dto.TrendMetricsDTO;
import com.stocktrading.repository.ExperimentSessionRepository;
import com.stocktrading.repository.ExperimentStockRepository;
import com.stocktrading.repository.ExperimentDecisionRepository;
import com.stocktrading.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private ExperimentSessionRepository sessionRepository;
    @Autowired private ExperimentStockRepository experimentStockRepository;
    @Autowired private ExperimentDecisionRepository decisionRepository;
    @Autowired private ExperimentService experimentService;
    @Autowired private MetricsCalculator metricsCalculator;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        User admin = getAdmin(auth);
        List<User> regularUsers = userService.getAllUsers().stream()
                .filter(u -> "USER".equals(u.getRole()))
                .toList();
        
        List<ExperimentSession> completedSessions = sessionRepository.findByCompletedTrue().stream()
                .filter(s -> s != null && s.getUser() != null)
                .toList();

        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", regularUsers.size());
        model.addAttribute("activeUsers", regularUsers.stream().filter(User::getActive).count());
        model.addAttribute("completedExps", completedSessions.size());
        model.addAttribute("recentSessions", completedSessions.stream().limit(10).toList());
        return "admin/dashboard";
    }


    @GetMapping("/experiments")
    public String experiments(Model model, Authentication auth) {
        User admin = getAdmin(auth);
<<<<<<< ours
        List<ExperimentSession> sessions = sessionRepository.findByCompletedTrue();
=======
        List<ExperimentSession> sessions = sessionRepository.findByCompletedTrue().stream()
                .filter(s -> s != null && s.getUser() != null)
                .toList();
>>>>>>> theirs

        model.addAttribute("admin", admin);
        model.addAttribute("sessions", sessions);
        model.addAttribute("totalStocks", ExperimentService.TOTAL_STOCKS);
        return "admin/experiments";
    }

    @GetMapping("/users")
    public String listUsers(Model model, Authentication auth) {
        User admin = getAdmin(auth);
        List<User> regularUsers = userService.getAllUsers().stream()
                .filter(u -> "USER".equals(u.getRole()))
                .toList();
        
        model.addAttribute("admin", admin);
        model.addAttribute("users", regularUsers);
        return "admin/users";
    }

    @GetMapping("/user/{id}")
    public String userDetail(@PathVariable Long id, Model model, Authentication auth) {
        User admin = getAdmin(auth);
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("admin", admin);
        model.addAttribute("user", user);
        return "admin/user-detail";
    }

    @GetMapping("/user/{id}/edit")
    public String editUser(@PathVariable Long id, Model model, Authentication auth) {
        User admin = getAdmin(auth);
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("admin", admin);
        model.addAttribute("user", user);
        return "admin/edit-user";
    }

    @PostMapping("/user/{id}/edit")
    public String updateUser(@PathVariable Long id,
                           @RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam(required = false) String password,
                           @RequestParam Double credits,
                           RedirectAttributes ra) {
        try {
            userService.updateUser(id, fullName, email, password, credits);
            ra.addFlashAttribute("success", "User updated successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/user/" + id;
    }

    @GetMapping("/users/add")
    public String addUserForm(Model model, Authentication auth) {
        model.addAttribute("admin", getAdmin(auth));
        return "admin/add-user";
    }

    @PostMapping("/users/add")
    public String createUser(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String fullName,
                           @RequestParam String email,
                           RedirectAttributes ra) {
        try {
            userService.createUser(username, password, fullName, email);
            ra.addFlashAttribute("success", "User created successfully");
            return "redirect:/admin/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/users/add";
        }
    }

    @PostMapping("/user/{id}/toggle")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.toggleUserStatus(id);
            ra.addFlashAttribute("success", "User status updated");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/user/" + id;
    }

    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if ("ADMIN".equals(user.getRole())) {
                ra.addFlashAttribute("error", "Cannot delete admin users");
                return "redirect:/admin/users";
            }
            
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/export-enhanced")
    @ResponseBody
    public ResponseEntity<byte[]> exportEnhancedData() {
        try {
            StringBuilder csv = new StringBuilder();

            csv.append("Username,Full Name,Email,Stock Number,Stock Symbol,Market Trend,Start Capital,End Capital,Return %,Buy Count,Sell Count,Total Trades,");
            csv.append("Trend Sharpe,Trend MaxDD,Trend Volatility,Trend WinRate,Trend Trades,Trend ProfitFactor,Trend TimeInMarket,");
            csv.append("Completed,Start Time,End Time\n");

            List<User> allUsers = userService.getAllUsers();
            List<ExperimentStock> stocks = experimentStockRepository.findAllByOrderBySequenceOrderAsc();

            for (User user : allUsers) {
                if ("ADMIN".equals(user.getRole())) continue;

                ExperimentSession session = experimentService.getAnySession(user);

                if (session == null) {
                    csv.append(String.format(Locale.US,
                        "%s,%s,%s,0,N/A,N/A,100000.00,100000.00,0.00,0,0,0,0.00,0.00,0.00,0.00,0,0.00,0.00,No,N/A,N/A\n",
                        csvField(user.getUsername()), csvField(user.getFullName()), csvField(user.getEmail())));
                    continue;
                }

                Map<MarketTrend, TrendMetricsDTO> trendMetrics = metricsCalculator.calculateTrendMetrics(session);
                List<ExperimentDecision> allDecisions = decisionRepository.findBySessionOrderByStockIndexAscDayNumberAsc(session);
                Map<Integer, List<ExperimentDecision>> decisionsByStock = allDecisions.stream()
                    .collect(java.util.stream.Collectors.groupingBy(ExperimentDecision::getStockIndex));

                double runningCapital = ExperimentService.INITIAL_CAPITAL;

                for (ExperimentStock stock : stocks) {
                    List<ExperimentDecision> stockDecisions = decisionsByStock
                        .getOrDefault(stock.getSequenceOrder(), java.util.Collections.emptyList());

                    StockRow stockRow = calculateStockRow(stockDecisions, runningCapital);
                    runningCapital = stockRow.endCapital;

                    long buyCount = stockDecisions.stream().filter(d -> "BUY".equalsIgnoreCase(d.getAction())).count();
                    long sellCount = stockDecisions.stream().filter(d -> "SELL".equalsIgnoreCase(d.getAction())).count();

                    MarketTrend trend = trendFromString(stock.getMarketTrend());
                    TrendMetricsDTO trendData = trendMetrics.getOrDefault(trend, new TrendMetricsDTO());

                    csv.append(String.format(Locale.US,
                        "%s,%s,%s,%d,%s,%s,%.2f,%.2f,%.2f,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%.2f,%s,%s,%s\n",
                        csvField(user.getUsername()),
                        csvField(user.getFullName()),
                        csvField(user.getEmail()),
                        stock.getSequenceOrder() + 1,
                        csvField(stock.getStockSymbol()),
                        csvField(stock.getMarketTrend()),
                        stockRow.startCapital,
                        stockRow.endCapital,
                        stockRow.returnPercent,
                        (int) buyCount,
                        (int) sellCount,
                        (int) (buyCount + sellCount),
                        trendData.getSharpeRatio(),
                        trendData.getMaxDrawdown(),
                        trendData.getVolatility(),
                        trendData.getWinRate(),
                        trendData.getNumberOfTrades(),
                        trendData.getProfitFactor(),
                        trendData.getAvgTimeInMarket(),
                        session.getCompleted() ? "Yes" : "No",
                        session.getStartTime() != null ? session.getStartTime().toString() : "N/A",
                        session.getEndTime() != null ? session.getEndTime().toString() : "N/A"
                    ));
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            String filename = "experiment_results_enhanced_" +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private StockRow calculateStockRow(List<ExperimentDecision> stockDecisions, double startCapital) {
        StockRow row = new StockRow();
        row.startCapital = startCapital;
        row.endCapital = startCapital;
        row.returnPercent = 0.0;

        if (stockDecisions == null || stockDecisions.isEmpty()) {
            return row;
        }

        List<ExperimentDecision> sorted = stockDecisions.stream()
            .sorted(java.util.Comparator.comparing(ExperimentDecision::getDayNumber))
            .toList();

        double cash = startCapital;
        int shares = 0;
        double lastPrice = sorted.get(0).getPrice() != null ? sorted.get(0).getPrice() : 0.0;

        for (ExperimentDecision decision : sorted) {
            String action = decision.getAction() != null ? decision.getAction().toUpperCase() : "HOLD";
            int qty = decision.getQuantity() != null ? decision.getQuantity() : 0;
            double price = decision.getPrice() != null ? decision.getPrice() : 0.0;
            lastPrice = price;

            if ("BUY".equals(action) && qty > 0) {
                cash -= (price * qty);
                shares += qty;
            } else if ("SELL".equals(action) && qty > 0) {
                int sellQty = Math.min(qty, shares);
                cash += (price * sellQty);
                shares -= sellQty;
            }
        }

        if (shares > 0) {
            cash += shares * lastPrice;
        }

        row.endCapital = cash;
        if (startCapital != 0) {
            row.returnPercent = ((cash - startCapital) / startCapital) * 100.0;
        }

        return row;
    }

    private MarketTrend trendFromString(String marketTrend) {
        if (marketTrend == null) {
            return MarketTrend.SIDEWAYS;
        }
        for (MarketTrend trend : MarketTrend.values()) {
            if (trend.name().equalsIgnoreCase(marketTrend) || trend.getDisplayName().equalsIgnoreCase(marketTrend)) {
                return trend;
            }
        }
        return MarketTrend.SIDEWAYS;
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static class StockRow {
        double startCapital;
        double endCapital;
        double returnPercent;
    }

    private User getAdmin(Authentication auth) {
        return userService.getUserByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }
}
