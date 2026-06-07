package com.online.attendance.sports.player.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreatePlayerRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long clubId;

    private LocalDate dateOfBirth;

    private BigDecimal height;

    private BigDecimal weight;

    private String position;

    private String medicalNotes;
}
