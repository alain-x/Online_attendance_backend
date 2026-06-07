package com.online.attendance.sports.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateMatchRequest {

    @NotNull
    private Long teamId;

    @NotBlank
    private String opponent;

    private String location;

    @NotNull
    private LocalDateTime matchDate;

    @NotBlank
    private String type;

    @NotBlank
    private String homeAway;
}
