package com.online.attendance.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Converts legacy PostgreSQL OID {@code @Lob} columns to BYTEA so company/logo reads stop failing.
 */
@Component
@Order(0)
public class PostgresBinaryColumnMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PostgresBinaryColumnMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public PostgresBinaryColumnMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        migrateColumn("companies", "logo_bytes");
        migrateColumn("system_branding", "logo_bytes");
        migrateColumn("system_branding", "favicon_bytes");
        migrateTextColumn("form_fields", "options_json");
        migrateTextColumn("form_submissions", "answers_json");
        migrateColumn("form_submission_files", "file_bytes");
        migrateColumn("employees", "profile_image_bytes");
    }

    private void migrateTextColumn(String table, String column) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE TEXT USING " + column + "::text"
            );
            log.info("Migrated {}.{} to TEXT", table, column);
        } catch (Exception ex) {
            log.debug("Skip migrating {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void migrateColumn(String table, String column) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE BYTEA USING NULL"
            );
            log.info("Migrated {}.{} to BYTEA", table, column);
        } catch (Exception ex) {
            log.debug("Skip migrating {}.{}: {}", table, column, ex.getMessage());
        }
    }
}
