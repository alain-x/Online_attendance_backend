package com.online.attendance.sports.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeamRequest {

    @NotBlank
    private String name;

    private String ageGroup;

    @NotNull
    private Long sportId;

    @NotNull
    private Long clubId;

    private Long coachId;

    private String description;
}
