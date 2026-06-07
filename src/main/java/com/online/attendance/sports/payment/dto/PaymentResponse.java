package com.online.attendance.sports.payment.dto;

import com.online.attendance.sports.payment.PlayerPayment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        Long feeId,
        String feeName,
        Long playerId,
        String playerName,
        BigDecimal amount,
        String currency,
        String status,
        LocalDate dueDate,
        LocalDate paidDate,
        String paymentMethod,
        String transactionRef,
        String notes,
        Instant createdAt
) {
    public static PaymentResponse from(PlayerPayment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getFee() != null ? payment.getFee().getId() : null,
                payment.getFee() != null ? payment.getFee().getName() : null,
                payment.getPlayer() != null ? payment.getPlayer().getId() : null,
                payment.getPlayer() != null && payment.getPlayer().getUser() != null ? payment.getPlayer().getUser().getUsername() : null,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getDueDate(),
                payment.getPaidDate(),
                payment.getPaymentMethod(),
                payment.getTransactionRef(),
                payment.getNotes(),
                payment.getCreatedAt()
        );
    }
}
