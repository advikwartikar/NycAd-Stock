package com.stocktrading.config;

import com.stocktrading.model.Stock;
import com.stocktrading.model.User;
import com.stocktrading.repository.StockRepository;
import com.stocktrading.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        System.out.println("========================================");
        System.out.println("LOADING INITIAL DATA");
        System.out.println("========================================");

        // Ensure admin users always exist and can log in
        ensureAdminUser("admin1", "admin123", "Admin User", "admin@stocktrading.com");
        ensureAdminUser("admin", "admin123", "Admin User", "admin2@stocktrading.com");
        System.out.println("Ensured admin users: admin1/admin123 and admin/admin123");

        // Create Real Users
        createUser("pranav", "pranav123", "Pranav Boke", "pranav.boke@gmail.com", "USER", 100000.0);
        createUser("ishan", "ishan123", "ISHAN ZALPURI", "ishan.zalp@gmail.com", "USER", 100000.0);
        createUser("sairaj", "sairaj123", "Sairaj Manoj Kasat", "sairaj.kasat@gmail.com", "USER", 100000.0);
        createUser("unnat", "unnat123", "Unnat Chandak", "chandakunnat@gmail.com", "USER", 100000.0);
        createUser("nikhil", "nikhil123", "Nikhil Biyani", "nikhhilbiyani24@gmail.com", "USER", 100000.0);
        createUser("mayur", "mayur123", "Mayur Chaudhari", "chaudharimayur6881@gmail.com", "USER", 100000.0);
        createUser("ashish", "ashish123", "Ashish Deshpande", "ashish.deshpande@stocktrading.local", "USER", 100000.0);
        createUser("rajesh", "rajesh123", "Rajesh T S", "rajesh.ts@stocktrading.local", "USER", 100000.0);
        createUser("nisha", "nisha123", "Nisha Lobo", "nisha.lobo@stocktrading.local", "USER", 100000.0);
        createUser("kinjal", "kinjal123", "Kinjal R Jetly", "kinjal.jetly@stocktrading.local", "USER", 100000.0);
        createUser("rohan", "rohan123", "Rohan Kaaat", "rohan.kaaat@stocktrading.local", "USER", 100000.0);
        createUser("vedashree", "vedashree123", "Vedashree Shisode", "vedashree.shisode@stocktrading.local", "USER", 100000.0);
        createUser("vedant", "vedant123", "Vedant Naatu", "vedant.naatu@stocktrading.local", "USER", 100000.0);
        createUser("kunal", "kunal123", "Kunal Kiwalkar", "kunal.kiwalkar@stocktrading.local", "USER", 100000.0);
        createUser("dummy", "dummy123", "Test Dummy User", "dummy.user@stocktrading.local", "USER", 100000.0);

        System.out.println("Created 15 real users:");
        System.out.println("  Pranav Boke (pranav/pranav123)");
        System.out.println("  ISHAN ZALPURI (ishan/ishan123)");
        System.out.println("  Sairaj Manoj Kasat (sairaj/sairaj123)");
        System.out.println("  Unnat Chandak (unnat/unnat123)");
        System.out.println("  Nikhil Biyani (nikhil/nikhil123)");
        System.out.println("  Mayur Chaudhari (mayur/mayur123)");
        System.out.println("  Ashish Deshpande (ashish/ashish123)");
        System.out.println("  Rajesh T S (rajesh/rajesh123)");
        System.out.println("  Nisha Lobo (nisha/nisha123)");
        System.out.println("  Kinjal R Jetly (kinjal/kinjal123)");
        System.out.println("  Rohan Kaaat (rohan/rohan123)");
        System.out.println("  Vedashree Shisode (vedashree/vedashree123)");
        System.out.println("  Vedant Naatu (vedant/vedant123)");
        System.out.println("  Kunal Kiwalkar (kunal/kunal123)");
        System.out.println("  Test Dummy User (dummy/dummy123)");

        // Load Stocks
        loadStocks();

        System.out.println("========================================");
    }

    private void createUser(String username, String password, String fullName, String email, String role, Double credits) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setEmail(email);
            user.setRole(role);
            user.setCredits(credits);
            user.setActive(true);
            userRepository.save(user);
        }
    }

    private void ensureAdminUser(String username, String rawPassword, String fullName, String email) {
        User admin = userRepository.findByUsername(username).orElseGet(User::new);
        admin.setUsername(username);
        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setRole("ADMIN");
        admin.setActive(true);
        admin.setCredits(0.0);

        boolean resetPassword = false;
        if (admin.getPassword() == null) {
            resetPassword = true;
        } else {
            try {
                resetPassword = !passwordEncoder.matches(rawPassword, admin.getPassword());
            } catch (Exception ignored) {
                // Handles legacy/plaintext/non-bcrypt values safely by resetting.
                resetPassword = true;
            }
        }

        if (resetPassword) {
            admin.setPassword(passwordEncoder.encode(rawPassword));
        }

        userRepository.save(admin);
    }

    private void loadStocks() {
        if (stockRepository.count() > 0) {
            System.out.println("Stocks already loaded: " + stockRepository.count());
            return;
        }

        List<Stock> stocks = new ArrayList<>();

        for (int i = 1; i <= 110; i++) {
            String csvPath = "data/stock_" + i + ".csv";
            InputStream is = getClass().getClassLoader().getResourceAsStream(csvPath);

            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    br.readLine();
                    String firstLine = br.readLine();

                    if (firstLine != null) {
                        String[] values = firstLine.split(",");

                        Stock stock = new Stock();
                        stock.setSymbol("STOCK_" + i);
                        stock.setName("Stock " + i);
                        stock.setCurrentPrice(Double.parseDouble(values[3]));
                        stock.setHighPrice(Double.parseDouble(values[1]));
                        stock.setLowPrice(Double.parseDouble(values[2]));
                        stock.setVolume(Long.parseLong(values[4].split("\\.")[0]));
                        stock.setChangePercent(0.0);
                        stock.setActive(true);

                        stocks.add(stock);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading stock_" + i + ".csv: " + e.getMessage());
                }
            }
        }

        stockRepository.saveAll(stocks);
        System.out.println("Stocks loaded successfully: " + stocks.size());
    }
}
