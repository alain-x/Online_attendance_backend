package com.online.attendance.sports.speed;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpeedSessionRepository extends JpaRepository<SpeedSession, Long> {
    List<SpeedSession> findByPlayerIdOrderByCreatedAtDesc(Long playerId);
    List<SpeedSession> findByPlayerIdAndStartTimeBetweenOrderByCreatedAtDesc(Long playerId, java.time.Instant start, java.time.Instant end);

    @EntityGraph(attributePaths = {"player", "player.club"})
    @Query("SELECT ss FROM SpeedSession ss JOIN ss.player p JOIN p.club c WHERE c.company.id = :companyId")
    List<SpeedSession> findByClubCompanyId(@Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"player", "player.club"})
    @Query("SELECT ss FROM SpeedSession ss JOIN ss.player p JOIN p.club c WHERE c.company.id = :companyId AND ss.id = :id")
    Optional<SpeedSession> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
