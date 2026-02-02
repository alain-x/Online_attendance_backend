package com.online.attendance.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DayAttendanceResponse {
    private String date;

    private long totalStaff;
    private long present;
    private long notIn;
    private long holidays;
    private long weeklyOff;

    private long workedMinutes;
    private long overtimeMinutes;

    private List<DayEmployeeRow> rows;
}
