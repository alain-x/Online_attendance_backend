package com.online.attendance.sports.training;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    @EntityGraph(attributePaths = {"team", "coach"})
    @Override
    List<TrainingSession> findAll();

    @EntityGraph(attributePaths = {"team", "coach"})
    List<TrainingSession> findByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team", "coach"})
    List<TrainingSession> findByCoachId(Long coachId);

    @EntityGraph(attributePaths = {"team", "coach"})
    List<TrainingSession> findByTeamIdAndStartTimeBetween(Long teamId, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"team", "coach"})
    @Query("SELECT ts FROM TrainingSession ts JOIN ts.team t JOIN t.club c WHERE c.company.id = :companyId")
    List<TrainingSession> findByClubCompanyId(@Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"team", "coach"})
    @Query("SELECT ts FROM TrainingSession ts JOIN ts.team t JOIN t.club c WHERE c.company.id = :companyId AND ts.id = :id")
    Optional<TrainingSession> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
