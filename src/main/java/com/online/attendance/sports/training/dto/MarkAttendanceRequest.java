package com.online.attendance.sports.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkAttendanceRequest {

    @NotNull
    private Long playerId;

    @NotBlank
    private String status;

    private String notes;
}
