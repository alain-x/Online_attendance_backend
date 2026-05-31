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

        ParsedPostgresUrl parsed = parsePostgresUrl(databaseUrl.trim());
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

    static ParsedPostgresUrl parsePostgresUrl(String databaseUrl) {
        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            throw new IllegalStateException("DATABASE_URL must use postgres:// or postgresql:// scheme");
        }

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
            throw new IllegalStateException("DATABASE_URL is missing database username");
        }

        return new ParsedPostgresUrl(jdbcUrl, username, password);
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
