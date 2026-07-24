package com.online.attendance.sports.speed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpeedSessionRepository extends JpaRepository<SpeedSession, Long> {
    List<SpeedSession> findByPlayerIdOrderByCreatedAtDesc(Long playerId);
    List<SpeedSession> findByPlayerIdAndStartTimeBetweenOrderByCreatedAtDesc(Long playerId, java.time.Instant start, java.time.Instant end);
}
