package com.online.attendance.sports.team.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTeamMemberRequest {

    @NotNull
    private Long playerId;

    private Integer jerseyNumber;

    private String position;
}
