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

    @EntityGraph(attributePaths = {"session", "player", "player.user"})
    @Query("SELECT ta FROM TrainingAttendance ta JOIN ta.session s JOIN s.team t JOIN t.club c WHERE c.company.id = :companyId")
    List<TrainingAttendance> findByClubCompanyId(@Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"session", "player", "player.user"})
    @Query("SELECT ta FROM TrainingAttendance ta JOIN ta.session s JOIN s.team t JOIN t.club c WHERE c.company.id = :companyId AND ta.id = :id")
    Optional<TrainingAttendance> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
