package com.online.attendance.sports.speed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpeedSessionRepository extends JpaRepository<SpeedSession, Long> {
    List<SpeedSession> findByPlayerIdOrderByCreatedAtDesc(Long playerId);
    List<SpeedSession> findByPlayerIdAndStartTimeBetweenOrderByCreatedAtDesc(Long playerId, java.time.Instant start, java.time.Instant end);

    @Query("SELECT ss FROM SpeedSession ss JOIN FETCH ss.player LEFT JOIN FETCH ss.player.club WHERE ss.player.club.company.id = :companyId")
    List<SpeedSession> findByClubCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT ss FROM SpeedSession ss JOIN FETCH ss.player LEFT JOIN FETCH ss.player.club WHERE ss.id = :id AND ss.player.club.company.id = :companyId")
    Optional<SpeedSession> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
