package com.online.attendance.sports.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    List<TrainingSession> findByTeamId(Long teamId);
    List<TrainingSession> findByCoachId(Long coachId);
    List<TrainingSession> findByTeamIdAndStartTimeBetween(Long teamId, LocalDateTime start, LocalDateTime end);
}
