package com.online.attendance.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TimesheetCell {
    /** PRESENT or OFF */
    private String state;

    private long workedMinutes;
    private long overtimeMinutes;

    /** Unpaid break minutes (closed breaks only). */
    private long breakMinutes;
}
