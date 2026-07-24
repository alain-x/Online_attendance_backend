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

    @EntityGraph(attributePaths = {"fee", "player", "player.user"})
    @Query("SELECT pp FROM PlayerPayment pp JOIN pp.player p JOIN p.club c WHERE c.company.id = :companyId")
    List<PlayerPayment> findByClubCompanyId(@Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"fee", "player", "player.user"})
    @Query("SELECT pp FROM PlayerPayment pp JOIN pp.player p JOIN p.club c WHERE c.company.id = :companyId AND pp.id = :id")
    Optional<PlayerPayment> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"fee", "player", "player.user"})
    @Query("SELECT pp FROM PlayerPayment pp WHERE pp.fee.team.id = :teamId")
    List<PlayerPayment> findByFeeTeamId(@Param("teamId") Long teamId);
}
