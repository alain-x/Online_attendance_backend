package com.online.attendance.sports.training;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrainingAttendanceRepository extends JpaRepository<TrainingAttendance, Long> {

    @EntityGraph(attributePaths = {"session", "player", "player.user"})
    @Override
    List<TrainingAttendance> findAll();

    @EntityGraph(attributePaths = {"session", "player", "player.user"})
    List<TrainingAttendance> findBySessionId(Long sessionId);

    @EntityGraph(attributePaths = {"session", "player", "player.user"})
    List<TrainingAttendance> findByPlayerId(Long playerId);

    @Query("SELECT ta FROM TrainingAttendance ta JOIN FETCH ta.session s JOIN FETCH s.team LEFT JOIN FETCH ta.player LEFT JOIN FETCH ta.player.user WHERE s.team.club.company.id = :companyId")
    List<TrainingAttendance> findByClubCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT ta FROM TrainingAttendance ta JOIN FETCH ta.session s JOIN FETCH s.team LEFT JOIN FETCH ta.player LEFT JOIN FETCH ta.player.user WHERE ta.id = :id AND s.team.club.company.id = :companyId")
    Optional<TrainingAttendance> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
