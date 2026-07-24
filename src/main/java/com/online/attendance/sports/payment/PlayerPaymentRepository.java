package com.online.attendance.sports.payment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @Query("SELECT pp FROM PlayerPayment pp JOIN FETCH pp.player LEFT JOIN FETCH pp.player.user LEFT JOIN FETCH pp.fee WHERE pp.player.club.company.id = :companyId")
    List<PlayerPayment> findByClubCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT pp FROM PlayerPayment pp JOIN FETCH pp.player LEFT JOIN FETCH pp.player.user LEFT JOIN FETCH pp.fee WHERE pp.id = :id AND pp.player.club.company.id = :companyId")
    Optional<PlayerPayment> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    @Query("SELECT pp FROM PlayerPayment pp WHERE pp.fee.team.id = :teamId")
    List<PlayerPayment> findByFeeTeamId(@Param("teamId") Long teamId);
}
