package com.online.attendance.sports.speed.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CreateSpeedSessionRequest {

    @NotNull
    private Long playerId;

    private String sessionName;

    @NotNull
    private Instant startTime;

    private Instant endTime;

    private Double totalDistanceMeters;

    private Double avgSpeedKmh;

    private Double maxSpeedKmh;

    private Integer durationSeconds;

    private Integer pointCount;

    private String gpsPoints;
}
