package com.online.attendance.sports.training.dto;

import com.online.attendance.sports.training.TrainingSession;

import java.time.LocalDateTime;

public record TrainingSessionResponse(
        Long id,
        Long teamId,
        String teamName,
        String title,
        String description,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long coachId,
        String coachName,
        String status,
        String notes,
        long attendanceCount
) {
    public static TrainingSessionResponse from(TrainingSession session, long attendanceCount) {
        return new TrainingSessionResponse(
                session.getId(),
                session.getTeam() != null ? session.getTeam().getId() : null,
                session.getTeam() != null ? session.getTeam().getName() : null,
                session.getTitle(),
                session.getDescription(),
                session.getLocation(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCoach() != null ? session.getCoach().getId() : null,
                session.getCoach() != null ? session.getCoach().getUsername() : null,
                session.getStatus(),
                session.getNotes(),
                attendanceCount
        );
    }
}
