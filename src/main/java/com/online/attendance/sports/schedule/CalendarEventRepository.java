package com.online.attendance.sports.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findByTeamId(Long teamId);
    List<CalendarEvent> findByTeamIdAndStartDateTimeBetween(Long teamId, LocalDateTime start, LocalDateTime end);
}
