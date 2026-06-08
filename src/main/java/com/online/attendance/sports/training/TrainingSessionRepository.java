package com.online.attendance.sports.training;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

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
}
