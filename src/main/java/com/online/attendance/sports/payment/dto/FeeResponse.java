package com.online.attendance.sports.payment.dto;

import com.online.attendance.sports.payment.MembershipFee;

import java.math.BigDecimal;

public record FeeResponse(
        Long id,
        Long clubId,
        String clubName,
        Long teamId,
        String teamName,
        String name,
        BigDecimal amount,
        String currency,
        String frequency,
        boolean active,
        String description
) {
    public static FeeResponse from(MembershipFee fee) {
        return new FeeResponse(
                fee.getId(),
                fee.getClub() != null ? fee.getClub().getId() : null,
                fee.getClub() != null ? fee.getClub().getName() : null,
                fee.getTeam() != null ? fee.getTeam().getId() : null,
                fee.getTeam() != null ? fee.getTeam().getName() : null,
                fee.getName(),
                fee.getAmount(),
                fee.getCurrency(),
                fee.getFrequency(),
                fee.isActive(),
                fee.getDescription()
        );
    }
}
