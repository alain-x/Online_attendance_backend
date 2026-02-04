package com.online.attendance.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class BulkTimesheetImportRequest {

    @NotNull
    @Valid
    private List<Row> rows;

    @Getter
    @Setter
    public static class Row {
        @NotNull
        private Long employeeId;

        @NotNull
        private Instant checkInTime;

        @NotNull
        private Instant checkOutTime;

        private Boolean locationVerified;
        private Boolean faceVerified;
    }
}
