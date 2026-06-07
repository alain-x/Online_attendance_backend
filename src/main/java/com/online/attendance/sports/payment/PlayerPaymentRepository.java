package com.online.attendance.sports.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerPaymentRepository extends JpaRepository<PlayerPayment, Long> {
    List<PlayerPayment> findByPlayerId(Long playerId);
    List<PlayerPayment> findByFeeId(Long feeId);
    List<PlayerPayment> findByStatus(String status);
}
