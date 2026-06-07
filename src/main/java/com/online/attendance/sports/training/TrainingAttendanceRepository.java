package com.online.attendance.sports.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingAttendanceRepository extends JpaRepository<TrainingAttendance, Long> {
    List<TrainingAttendance> findBySessionId(Long sessionId);
    List<TrainingAttendance> findByPlayerId(Long playerId);
}
