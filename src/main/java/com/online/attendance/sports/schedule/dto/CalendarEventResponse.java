package com.online.attendance.sports.schedule.dto;

import com.online.attendance.sports.schedule.CalendarEvent;

import java.time.LocalDateTime;

public record CalendarEventResponse(
        Long id,
        Long teamId,
        String teamName,
        String title,
        String description,
        String eventType,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String location,
        boolean allDay,
        String color,
        Long createdById,
        String createdByName
) {
    public static CalendarEventResponse from(CalendarEvent event) {
        return new CalendarEventResponse(
                event.getId(),
                event.getTeam() != null ? event.getTeam().getId() : null,
                event.getTeam() != null ? event.getTeam().getName() : null,
                event.getTitle(),
                event.getDescription(),
                event.getEventType(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.getLocation(),
                event.isAllDay(),
                event.getColor(),
                event.getCreatedBy() != null ? event.getCreatedBy().getId() : null,
                event.getCreatedBy() != null ? event.getCreatedBy().getUsername() : null
        );
    }
}
