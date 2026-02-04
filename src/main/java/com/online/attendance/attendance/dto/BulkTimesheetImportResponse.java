package com.online.attendance.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BulkTimesheetImportResponse {

    private final int ok;
    private final int failed;
    private final List<RowResult> results;

    @Getter
    @AllArgsConstructor
    public static class RowResult {
        private final int index;
        private final Long employeeId;
        private final boolean ok;
        private final String message;
        private final Long attendanceId;
    }
}
