package com.online.attendance.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TimesheetEmployeeRow {
    private Long employeeId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String department;
    private String role;

    /** One cell per day in the response's days list */
    private List<TimesheetCell> days;

    private long presentDays;
    private long offDays;

    private long workedMinutes;
    private long overtimeMinutes;

    private long breakMinutes;
}
