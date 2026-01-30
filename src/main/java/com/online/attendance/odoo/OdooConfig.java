package com.online.attendance.odoo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "odoo")
public class OdooConfig {
    /**
     * Base URL of Odoo instance (e.g. https://odoo.example.com).
     */
    private String baseUrl;

    /**
     * Odoo database name.
     */
    private String db;

    /**
     * Integration username (service account).
     */
    private String username;

    /**
     * Integration password / API key.
     */
    private String password;

    /**
     * Enable/disable sync (safe default is false).
     */
    private boolean enabled = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

