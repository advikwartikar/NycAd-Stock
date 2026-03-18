package com.stocktrading.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseUrlNormalizerConfig implements BeanFactoryPostProcessor, EnvironmentAware {

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            this.environment = configurableEnvironment;
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (environment == null) {
            return;
        }

        String configuredUrl = environment.getProperty("spring.datasource.url");
        String databaseUrl = environment.getProperty("DATABASE_URL");
        String sourceUrl = isBlank(configuredUrl) ? databaseUrl : configuredUrl;

        if (isBlank(sourceUrl)) {
            return;
        }

        ParsedDb parsed = parse(sourceUrl);
        if (parsed == null || isBlank(parsed.jdbcUrl)) {
            return;
        }

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("spring.datasource.url", parsed.jdbcUrl);
        overrides.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

        String existingUser = environment.getProperty("spring.datasource.username");
        String existingPass = environment.getProperty("spring.datasource.password");

        if (isBlank(existingUser) && !isBlank(parsed.username)) {
            overrides.put("spring.datasource.username", parsed.username);
        }
        if (isBlank(existingPass) && !isBlank(parsed.password)) {
            overrides.put("spring.datasource.password", parsed.password);
        }

        if (!overrides.isEmpty()) {
            MutablePropertySources sources = environment.getPropertySources();
            sources.addFirst(new MapPropertySource("databaseUrlNormalizer", overrides));
        }
    }

    private ParsedDb parse(String raw) {
        try {
            String value = raw.trim();

            // Handles malformed value like:
            // jdbc:postgresql://user:pass@host:5432/db
            if (value.startsWith("jdbc:postgresql://") && value.contains("@")) {
                String noJdbc = value.substring("jdbc:".length()); // postgresql://...
                return parseUriStyle("postgres://" + noJdbc.substring("postgresql://".length()));
            }

            // Handles DATABASE_URL style:
            // postgres://user:pass@host:5432/db
            if (value.startsWith("postgres://")) {
                return parseUriStyle(value);
            }

            // Already valid JDBC URL without embedded credentials
            if (value.startsWith("jdbc:postgresql://")) {
                ParsedDb db = new ParsedDb();
                db.jdbcUrl = value;
                return db;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private ParsedDb parseUriStyle(String postgresUri) {
        URI uri = URI.create(postgresUri);

        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath(); // includes leading '/'
        String query = uri.getQuery();

        String jdbcUrl = "jdbc:postgresql://" + host + (port > 0 ? ":" + port : "") + path;
        if (!isBlank(query)) {
            jdbcUrl += "?" + query;
        }

        String username = null;
        String password = null;
        String userInfo = uri.getUserInfo();
        if (!isBlank(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            username = parts.length > 0 ? parts[0] : null;
            password = parts.length > 1 ? parts[1] : null;
        }

        ParsedDb db = new ParsedDb();
        db.jdbcUrl = jdbcUrl;
        db.username = username;
        db.password = password;
        return db;
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static class ParsedDb {
        String jdbcUrl;
        String username;
        String password;
    }
}

