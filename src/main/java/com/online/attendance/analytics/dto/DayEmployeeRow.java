package com.online.attendance.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DayEmployeeRow {
    private Long employeeId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String department;
    private String role;

    /** ISO-8601 instant string, UTC */
    private String inTime;

    /** ISO-8601 instant string, UTC */
    private String outTime;

    private long workedMinutes;
    private long overtimeMinutes;

    /** IN, OUT, NOT_IN */
    private String status;
}
