package com.online.attendance.user;

import java.util.Optional;

public enum Role {
    SYSTEM_ADMIN,
    ADMIN,
    HR,
    MANAGER,
    RECORDER,
    EMPLOYEE,
    PAYROLL,
    AUDITOR,
    CLUB_ADMIN,
    COACH,
    TEAM_MANAGER,
    PLAYER,
    PARENT;

    public static Optional<Role> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Role.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
