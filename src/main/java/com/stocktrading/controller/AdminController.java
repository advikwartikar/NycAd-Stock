package com.stocktrading.controller;

import com.stocktrading.model.User;
import com.stocktrading.model.ExperimentSession;
import com.stocktrading.model.ExperimentDecision;
import com.stocktrading.model.MarketTrend;
import com.stocktrading.dto.TrendMetricsDTO;
import com.stocktrading.repository.ExperimentDecisionRepository;
import com.stocktrading.repository.ExperimentSessionRepository;
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
import java.util.Comparator;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private ExperimentSessionRepository sessionRepository;
    @Autowired private ExperimentDecisionRepository decisionRepository;
    @Autowired private ExperimentService experimentService;
    @Autowired private MetricsCalculator metricsCalculator;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        model.addAttribute("admin", getAdmin(auth));
        try {
            List<User> regularUsers = userService.getAllUsers().stream()
                    .filter(u -> "USER".equals(u.getRole()))
                    .toList();
            List<ExperimentSession> completedSessions = sessionRepository.findByCompletedTrue();

            model.addAttribute("totalUsers", regularUsers.size());
            model.addAttribute("activeUsers", regularUsers.stream().filter(User::getActive).count());
            model.addAttribute("completedExps", completedSessions.size());
            model.addAttribute("recentSessions", completedSessions.stream().limit(10).toList());
        } catch (Exception e) {
            model.addAttribute("totalUsers", 0);
            model.addAttribute("activeUsers", 0);
            model.addAttribute("completedExps", 0);
            model.addAttribute("recentSessions", List.of());
            model.addAttribute("error", "Unable to load dashboard stats: " + e.getMessage());
        }
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model, Authentication auth) {
        model.addAttribute("admin", getAdmin(auth));
        try {
            List<User> regularUsers = userService.getAllUsers().stream()
                    .filter(u -> "USER".equals(u.getRole()))
                    .toList();
            model.addAttribute("users", regularUsers);
        } catch (Exception e) {
            model.addAttribute("users", List.of());
            model.addAttribute("error", "Unable to load users: " + e.getMessage());
        }
        return "admin/users";
    }

    @GetMapping("/user/{id}")
    public String userDetail(@PathVariable Long id, Model model, Authentication auth) {
        model.addAttribute("admin", getAdmin(auth));
        try {
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            List<ExperimentSession> completedSessions = sessionRepository.findByUser(user).stream()
                    .filter(s -> Boolean.TRUE.equals(s.getCompleted()))
                    .toList();

            model.addAttribute("viewUser", user);
            model.addAttribute("completedSessions", completedSessions);
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load user details: " + e.getMessage());
            return "redirect:/admin/users";
        }
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

    @PostMapping("/user/{id}/toggle-status")
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
            
            csv.append("Username,Full Name,Email,Total Stocks,Total Decisions,Final Capital,Total P/L,");
            csv.append("Bullish Sharpe,Bullish MaxDD,Bullish Volatility,Bullish WinRate,Bullish Trades,Bullish ProfitFactor,Bullish TimeInMarket,");
            csv.append("Bearish Sharpe,Bearish MaxDD,Bearish Volatility,Bearish WinRate,Bearish Trades,Bearish ProfitFactor,Bearish TimeInMarket,");
            csv.append("Sideways Sharpe,Sideways MaxDD,Sideways Volatility,Sideways WinRate,Sideways Trades,Sideways ProfitFactor,Sideways TimeInMarket,");
            csv.append("Completed,Start Time,End Time\n");
            
            List<User> allUsers = userService.getAllUsers();
            
            for (User user : allUsers) {
                try {
                    if ("ADMIN".equals(user.getRole())) continue;

                    List<ExperimentDecision> allUserDecisions = decisionRepository.findAllByUserId(user.getId());
                    long totalDecisions = allUserDecisions.size();
                    long stocksTraded = allUserDecisions.stream()
                            .map(ExperimentDecision::getStockIndex)
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .count();

                    ExperimentSession session = getBestSessionForExport(user);

                    if (session == null) {
                        csv.append(String.format("%s,%s,%s,%d,%d,100000,0,",
                                user.getUsername(), user.getFullName(), user.getEmail(), stocksTraded, totalDecisions));
                        csv.append("0,0,0,0,0,0,0,");
                        csv.append("0,0,0,0,0,0,0,");
                        csv.append("0,0,0,0,0,0,0,");
                        csv.append("No,N/A,N/A\n");
                        continue;
                    }

                    Map<MarketTrend, TrendMetricsDTO> trendMetrics = metricsCalculator.calculateTrendMetrics(session);

                    TrendMetricsDTO bullish = trendMetrics.getOrDefault(MarketTrend.BULLISH, new TrendMetricsDTO());
                    TrendMetricsDTO bearish = trendMetrics.getOrDefault(MarketTrend.BEARISH, new TrendMetricsDTO());
                    TrendMetricsDTO sideways = trendMetrics.getOrDefault(MarketTrend.SIDEWAYS, new TrendMetricsDTO());

                    double finalCapital = session.getCurrentCapital() != null ? session.getCurrentCapital() : 100000.0;
                    double totalPL = finalCapital - 100000.0;

                    csv.append(String.format("%s,%s,%s,%d,%d,%.2f,%.2f,",
                            user.getUsername(), user.getFullName(), user.getEmail(),
                            stocksTraded, totalDecisions, finalCapital, totalPL));

                    csv.append(String.format("%.2f,%.2f,%.2f,%.2f,%d,%.2f,%.2f,",
                            bullish.getSharpeRatio(), bullish.getMaxDrawdown(), bullish.getVolatility(),
                            bullish.getWinRate(), bullish.getNumberOfTrades(), bullish.getProfitFactor(),
                            bullish.getAvgTimeInMarket()));

                    csv.append(String.format("%.2f,%.2f,%.2f,%.2f,%d,%.2f,%.2f,",
                            bearish.getSharpeRatio(), bearish.getMaxDrawdown(), bearish.getVolatility(),
                            bearish.getWinRate(), bearish.getNumberOfTrades(), bearish.getProfitFactor(),
                            bearish.getAvgTimeInMarket()));

                    csv.append(String.format("%.2f,%.2f,%.2f,%.2f,%d,%.2f,%.2f,",
                            sideways.getSharpeRatio(), sideways.getMaxDrawdown(), sideways.getVolatility(),
                            sideways.getWinRate(), sideways.getNumberOfTrades(), sideways.getProfitFactor(),
                            sideways.getAvgTimeInMarket()));

                    csv.append(String.format("%s,%s,%s\n",
                            session.getCompleted() ? "Yes" : "No",
                            session.getStartTime() != null ? session.getStartTime().toString() : "N/A",
                            session.getEndTime() != null ? session.getEndTime().toString() : "N/A"));
                } catch (Exception userEx) {
                    csv.append(String.format("%s,%s,%s,0,0,100000,0,",
                            user.getUsername(), user.getFullName(), user.getEmail()));
                    csv.append("0,0,0,0,0,0,0,");
                    csv.append("0,0,0,0,0,0,0,");
                    csv.append("0,0,0,0,0,0,0,");
                    csv.append("No,ERROR,ERROR\n");
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

    private ExperimentSession getBestSessionForExport(User user) {
        List<ExperimentSession> sessions = sessionRepository.findByUser(user);
        if (sessions.isEmpty()) {
            return null;
        }

        return sessions.stream()
                .max(Comparator
                        .comparing((ExperimentSession s) -> Boolean.TRUE.equals(s.getCompleted()))
                        .thenComparingInt(s -> s.getCurrentStockIndex() == null ? 0 : s.getCurrentStockIndex())
                        .thenComparingLong((ExperimentSession s) -> decisionRepository.countBySessionId(s.getId()))
                        .thenComparing(ExperimentSession::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private User getAdmin(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userService.getUserByUsername(auth.getName()).orElse(null);
    }

    @ExceptionHandler(Exception.class)
    public String handleAdminException(Exception e, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Admin error: " + e.getMessage());
        return "redirect:/admin/dashboard";
    }
}
