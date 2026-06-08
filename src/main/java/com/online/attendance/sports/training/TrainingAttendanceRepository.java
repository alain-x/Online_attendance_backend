package com.online.attendance.sports.training;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingAttendanceRepository extends JpaRepository<TrainingAttendance, Long> {

    @EntityGraph(attributePaths = {"session", "player", "player.user"})
    @Override
    List<TrainingAttendance> findAll();

    @EntityGraph(attributePaths = {"session", "player", "player.user"})
    List<TrainingAttendance> findBySessionId(Long sessionId);

    @EntityGraph(attributePaths = {"session", "player", "player.user"})
    List<TrainingAttendance> findByPlayerId(Long playerId);
}
