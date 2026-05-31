package com.online.attendance.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts Render's {@code postgres://} DATABASE_URL into Spring JDBC properties.
 */
public class RenderDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        String trimmed = databaseUrl.trim();
        if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) {
            return;
        }

        try {
            URI uri = URI.create(trimmed.replace("postgres://", "postgresql://"));
            String jdbcUrl = buildJdbcUrl(uri);
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbcUrl);

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                props.put("spring.datasource.username", decode(parts[0]));
                if (parts.length > 1) {
                    props.put("spring.datasource.password", decode(parts[1]));
                }
            }

            environment.getPropertySources().addFirst(new MapPropertySource("renderDatabase", props));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to parse DATABASE_URL for Render PostgreSQL", ex);
        }
    }

    private static String buildJdbcUrl(URI uri) {
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
        jdbc.append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append(uri.getPath());
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            jdbc.append('?').append(uri.getQuery());
        }
        return jdbc.toString();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
