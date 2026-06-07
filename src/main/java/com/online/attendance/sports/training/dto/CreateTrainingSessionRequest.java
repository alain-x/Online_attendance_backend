package com.online.attendance.sports.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateTrainingSessionRequest {

    @NotNull
    private Long teamId;

    @NotBlank
    private String title;

    private String description;

    private String location;

    @NotNull
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long coachId;
}
