package com.online.attendance.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TimesheetResponse {
    private int year;
    private int month;

    private String from;
    private String to;

    /** LocalDate strings (yyyy-MM-dd) in UTC */
    private List<String> days;

    private List<TimesheetEmployeeRow> rows;
}
