package com.online.attendance.sports.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecordPaymentRequest {

    @NotNull
    private Long feeId;

    @NotNull
    private Long playerId;

    @NotNull
    private BigDecimal amount;

    private LocalDate dueDate;

    private String notes;
}
