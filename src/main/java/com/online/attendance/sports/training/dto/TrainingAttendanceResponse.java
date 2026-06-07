package com.online.attendance.sports.training.dto;

import com.online.attendance.sports.training.TrainingAttendance;

public record TrainingAttendanceResponse(
        Long id,
        Long sessionId,
        Long playerId,
        String playerName,
        String status,
        String notes
) {
    public static TrainingAttendanceResponse from(TrainingAttendance attendance) {
        return new TrainingAttendanceResponse(
                attendance.getId(),
                attendance.getSession() != null ? attendance.getSession().getId() : null,
                attendance.getPlayer() != null ? attendance.getPlayer().getId() : null,
                attendance.getPlayer() != null && attendance.getPlayer().getUser() != null ? attendance.getPlayer().getUser().getUsername() : null,
                attendance.getStatus(),
                attendance.getNotes()
        );
    }
}
