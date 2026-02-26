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

    @Autowired private UserRepository userRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        System.out.println("========================================");
        System.out.println("LOADING INITIAL DATA");
        System.out.println("========================================");

        // Create Admin User
        createUser("admin1", "admin123", "Admin User", "admin@stocktrading.com", "ADMIN", 0.0);
        System.out.println("✓ Created admin user: admin1");

        // Create Real Users
        createUser("pranav", "pranav123", "Pranav Boke", "pranav.boke@gmail.com", "USER", 100000.0);
        createUser("ishan", "ishan123", "ISHAN ZALPURI", "ishan.zalp@gmail.com", "USER", 100000.0);
        createUser("sairaj", "sairaj123", "Sairaj Manoj Kasat", "sairaj.kasat@gmail.com", "USER", 100000.0);
        createUser("unnat", "unnat123", "Unnat Chandak", "chandakunnat@gmail.com", "USER", 100000.0);
        createUser("nikhil", "nikhil123", "Nikhil Biyani", "nikhhilbiyani24@gmail.com", "USER", 100000.0);
        createUser("mayur", "mayur123", "Mayur Chaudhari", "chaudharimayur6881@gmail.com", "USER", 100000.0);

        System.out.println("✓ Created 6 real users:");
        System.out.println("  Pranav Boke (pranav/pranav123)");
        System.out.println("  ISHAN ZALPURI (ishan/ishan123)");
        System.out.println("  Sairaj Manoj Kasat (sairaj/sairaj123)");
        System.out.println("  Unnat Chandak (unnat/unnat123)");
        System.out.println("  Nikhil Biyani (nikhil/nikhil123)");
        System.out.println("  Mayur Chaudhari (mayur/mayur123)");

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

    private void loadStocks() {
        if (stockRepository.count() > 0) {
            System.out.println("✓ Stocks already loaded: " + stockRepository.count());
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
        System.out.println("✓ Stocks loaded successfully: " + stocks.size());
    }
}