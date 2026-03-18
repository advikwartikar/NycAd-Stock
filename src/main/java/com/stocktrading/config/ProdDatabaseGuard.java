package com.stocktrading.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;

@Component
@Order(0)
public class ProdDatabaseGuard implements CommandLineRunner {

    private final Environment environment;
    private final DataSource dataSource;

    public ProdDatabaseGuard(Environment environment, DataSource dataSource) {
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean prodActive = Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);

        if (!prodActive) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            String url = meta.getURL() == null ? "" : meta.getURL().toLowerCase();
            String product = meta.getDatabaseProductName() == null ? "" : meta.getDatabaseProductName().toLowerCase();

            boolean postgres = url.contains("postgresql") || product.contains("postgresql");
            if (!postgres) {
                throw new IllegalStateException(
                        "Production profile requires PostgreSQL, but connected to: URL='" +
                                meta.getURL() + "', product='" + meta.getDatabaseProductName() + "'.");
            }
        }
    }
}

