package com.stocktrading.controller;

import com.stocktrading.model.User;
import com.stocktrading.model.ExperimentSession;
import com.stocktrading.repository.ExperimentStockRepository;
import com.stocktrading.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired private UserService userService;
    @Autowired private ExperimentService experimentService;
    @Autowired private ExperimentStockRepository experimentStockRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);

        // ADMIN: Redirect to admin panel (no experiment for admins)
        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        }

        // USER: Check experiment status
        long stockCount = experimentStockRepository.count();
        model.addAttribute("experimentReady", stockCount >= 10);
        model.addAttribute("stockCount", stockCount);

        ExperimentSession activeSession = experimentService.getCurrentSession(user);
        if (activeSession != null && !activeSession.getCompleted()) {
            return "redirect:/experiment/trade";
        }

        ExperimentSession anySession = experimentService.getAnySession(user);
        if (anySession != null && anySession.getCompleted()) {
            model.addAttribute("experimentCompleted", true);
            model.addAttribute("session", anySession);
        } else {
            model.addAttribute("canStartExperiment", true);
        }

        return "dashboard";
    }
}
