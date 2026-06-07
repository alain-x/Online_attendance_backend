package com.online.attendance.sports.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddMatchEventRequest {

    @NotNull
    private Long playerId;

    @NotBlank
    private String eventType;

    private Integer minute;

    private String notes;
}
