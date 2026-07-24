package com.online.attendance.sports.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEvaluationRequest {

    @NotNull
    private Long playerId;

    @NotNull
    private Long teamId;

    private String period;

    @Min(1)
    @Max(10)
    private Integer overallRating;

    private String coachNotes;

    private String goals;

    private Double avgSpeedKmh;

    private Double maxSpeedKmh;

    private Double totalDistanceKm;

    private Integer totalTrainingMinutes;
}
