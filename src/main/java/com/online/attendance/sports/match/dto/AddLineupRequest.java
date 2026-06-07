package com.online.attendance.sports.match.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddLineupRequest {

    @NotNull
    private Long playerId;

    private Integer jerseyNumber;

    private String position;

    private boolean isStarter;
}
