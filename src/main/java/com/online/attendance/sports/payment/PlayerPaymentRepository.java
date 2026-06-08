package com.online.attendance.sports.payment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerPaymentRepository extends JpaRepository<PlayerPayment, Long> {

    @EntityGraph(attributePaths = {"fee", "player", "player.user"})
    @Override
    List<PlayerPayment> findAll();

    @EntityGraph(attributePaths = {"fee", "player", "player.user"})
    List<PlayerPayment> findByPlayerId(Long playerId);

    @EntityGraph(attributePaths = {"fee", "player", "player.user"})
    List<PlayerPayment> findByFeeId(Long feeId);

    @EntityGraph(attributePaths = {"fee", "player", "player.user"})
    List<PlayerPayment> findByStatus(String status);
}
