package com.stocktrading.controller;

import com.stocktrading.model.User;
import com.stocktrading.model.ExperimentSession;
import com.stocktrading.repository.ExperimentSessionRepository;
import com.stocktrading.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private ExperimentSessionRepository sessionRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        User admin = getAdmin(auth);
        List<User> regularUsers = userService.getAllUsers().stream()
                .filter(u -> "USER".equals(u.getRole()))
                .toList();
        
        List<ExperimentSession> completedSessions = sessionRepository.findByCompletedTrue();

        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", regularUsers.size());
        model.addAttribute("activeUsers", regularUsers.stream().filter(User::getActive).count());
        model.addAttribute("completedExps", completedSessions.size());
        model.addAttribute("recentSessions", completedSessions.stream().limit(10).toList());
        return "admin/dashboard";
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
        User viewUser = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<ExperimentSession> sessions = sessionRepository.findByUser(viewUser);
        
        model.addAttribute("admin", admin);
        model.addAttribute("viewUser", viewUser);
        model.addAttribute("sessions", sessions);
        model.addAttribute("completedSessions", sessions.stream()
                .filter(ExperimentSession::getCompleted)
                .toList());
        return "admin/user-detail";
    }

    @GetMapping("/experiments")
    public String experiments(Model model, Authentication auth) {
        User admin = getAdmin(auth);
        List<ExperimentSession> sessions = sessionRepository.findByCompletedTrue();
        
        model.addAttribute("admin", admin);
        model.addAttribute("sessions", sessions);
        return "admin/experiments";
    }

    @PostMapping("/user/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setActive(!user.getActive());
            userService.updateUser(user);
            
            String status = user.getActive() ? "activated" : "deactivated";
            ra.addFlashAttribute("success", user.getUsername() + " " + status);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/users";
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

    private User getAdmin(Authentication auth) {
        return userService.getUserByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }
}
