package com.online.attendance.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RoleCheckConstraintMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleCheckConstraintMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public RoleCheckConstraintMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            String constraintName = "users_role_check";
            String allRoles = "'SYSTEM_ADMIN','ADMIN','HR','MANAGER','RECORDER','EMPLOYEE','PAYROLL','AUDITOR','CLUB_ADMIN','COACH','TEAM_MANAGER','PLAYER','PARENT'";

            String checkExisting = "SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = ? AND table_name = 'users' AND constraint_type = 'CHECK'";
            boolean exists = jdbcTemplate.query(checkExisting, rs -> rs.next() ? rs.getInt(1) == 1 : false, constraintName);

            if (exists) {
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT " + constraintName);
                log.info("Dropped existing constraint: {}", constraintName);
            }

            jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT " + constraintName + " CHECK (role IN (" + allRoles + "))");
            log.info("Recreated constraint: {} with all role values", constraintName);
        } catch (Exception e) {
            log.warn("Could not migrate role check constraint: {}", e.getMessage());
        }
    }
}
