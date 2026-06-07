package com.online.attendance.sports.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateFeeRequest {

    @NotNull
    private Long clubId;

    private Long teamId;

    @NotBlank
    private String name;

    @NotNull
    private BigDecimal amount;

    private String currency;

    @NotBlank
    private String frequency;

    private String description;
}
