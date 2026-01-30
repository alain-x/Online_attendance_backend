package com.online.attendance.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BreakRepository extends JpaRepository<BreakRecord, Long> {
    Optional<BreakRecord> findTopByAttendanceRecordIdAndBreakEndTimeIsNullOrderByBreakStartTimeDesc(Long attendanceId);
    List<BreakRecord> findByAttendanceRecordIdOrderByBreakStartTimeAsc(Long attendanceId);
}
