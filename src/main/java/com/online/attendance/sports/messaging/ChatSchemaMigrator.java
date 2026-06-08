package com.online.attendance.sports.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
public class ChatSchemaMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ChatSchemaMigrator.class);
    private final JdbcTemplate jdbcTemplate;

    public ChatSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        addColumnIfMissing("sports_chat_rooms", "is_group", "BOOLEAN DEFAULT FALSE");
        addColumnIfMissing("sports_chat_messages", "file_name", "VARCHAR(255)");
        addColumnIfMissing("sports_chat_messages", "file_size", "BIGINT");
        addColumnIfMissing("sports_chat_messages", "mime_type", "VARCHAR(100)");

        // Ensure FK columns are nullable (schema may have been created with NOT NULL previously)
        setNullable("sports_chat_rooms", "team_id");
        setNullable("sports_chat_rooms", "created_by");
    }

    private void addColumnIfMissing(String table, String column, String type) {
        try {
            List<String> cols = jdbcTemplate.query(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                    (rs, i) -> rs.getString("column_name"),
                    table, column
            );
            if (cols.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
                log.info("Added column {}.{} ({})", table, column, type);
            }
        } catch (Exception e) {
            log.warn("Could not add column {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void setNullable(String table, String column) {
        try {
            List<String> notNull = jdbcTemplate.query(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = ? AND column_name = ? AND is_nullable = 'NO'",
                    (rs, i) -> rs.getString("column_name"),
                    table, column
            );
            if (!notNull.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " DROP NOT NULL");
                log.info("Dropped NOT NULL on {}.{}", table, column);
            }
        } catch (Exception e) {
            log.warn("Could not drop NOT NULL on {}.{}: {}", table, column, e.getMessage());
        }
    }
}
