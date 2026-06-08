package com.online.attendance.sports.schedule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @EntityGraph(attributePaths = {"team", "createdBy"})
    @Override
    List<CalendarEvent> findAll();

    @EntityGraph(attributePaths = {"team", "createdBy"})
    List<CalendarEvent> findByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team", "createdBy"})
    List<CalendarEvent> findByTeamIdAndStartDateTimeBetween(Long teamId, LocalDateTime start, LocalDateTime end);
}
