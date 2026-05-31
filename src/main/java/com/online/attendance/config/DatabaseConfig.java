package com.online.attendance.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource renderDataSource(DataSourceProperties defaults) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (!StringUtils.hasText(databaseUrl)) {
            throw new IllegalStateException("DATABASE_URL is set but empty");
        }

        ParsedPostgresUrl parsed = parseDatabaseUrl(
                databaseUrl.trim(),
                System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD")
        );

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(parsed.jdbcUrl());
        config.setUsername(parsed.username());
        config.setPassword(parsed.password());
        config.setDriverClassName(defaults.getDriverClassName() != null
                ? defaults.getDriverClassName()
                : "org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    static ParsedPostgresUrl parseDatabaseUrl(String databaseUrl, String fallbackUser, String fallbackPassword) {
        if (databaseUrl.startsWith("jdbc:postgresql://") || databaseUrl.startsWith("jdbc:postgres://")) {
            String jdbcUrl = ensureSslMode(databaseUrl);
            if (!StringUtils.hasText(fallbackUser)) {
                throw new IllegalStateException("DATABASE_URL is JDBC format but DB_USER is not set");
            }
            return new ParsedPostgresUrl(
                    jdbcUrl,
                    fallbackUser,
                    fallbackPassword != null ? fallbackPassword : ""
            );
        }

        if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            URI uri = URI.create(databaseUrl.replace("postgres://", "postgresql://"));
            String jdbcUrl = buildJdbcUrl(uri);

            String username = null;
            String password = "";
            String userInfo = uri.getUserInfo();
            if (StringUtils.hasText(userInfo)) {
                String[] parts = userInfo.split(":", 2);
                username = decode(parts[0]);
                if (parts.length > 1) {
                    password = decode(parts[1]);
                }
            }
            if (!StringUtils.hasText(username)) {
                username = fallbackUser;
            }
            if (!StringUtils.hasText(password) && fallbackPassword != null) {
                password = fallbackPassword;
            }
            if (!StringUtils.hasText(username)) {
                throw new IllegalStateException("DATABASE_URL is missing database username");
            }
            return new ParsedPostgresUrl(jdbcUrl, username, password);
        }

        throw new IllegalStateException(
                "DATABASE_URL must use jdbc:postgresql://, postgres://, or postgresql:// scheme");
    }

    private static String ensureSslMode(String jdbcUrl) {
        if (jdbcUrl.contains("sslmode=")) {
            return jdbcUrl;
        }
        return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
    }

    private static String buildJdbcUrl(URI uri) {
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
        jdbc.append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append(uri.getPath());
        if (StringUtils.hasText(uri.getQuery())) {
            jdbc.append('?').append(uri.getQuery());
        } else {
            jdbc.append("?sslmode=require");
        }
        return jdbc.toString();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record ParsedPostgresUrl(String jdbcUrl, String username, String password) {}
}
