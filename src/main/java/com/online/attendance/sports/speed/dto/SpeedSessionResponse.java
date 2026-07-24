package com.online.attendance.sports.speed.dto;

import com.online.attendance.sports.speed.SpeedSession;

import java.time.Instant;

public record SpeedSessionResponse(
        Long id,
        Long playerId,
        String playerName,
        String sessionName,
        Instant startTime,
        Instant endTime,
        Double totalDistanceMeters,
        Double avgSpeedKmh,
        Double maxSpeedKmh,
        Integer durationSeconds,
        Integer pointCount,
        Instant createdAt
) {
    public static SpeedSessionResponse from(SpeedSession session) {
        return new SpeedSessionResponse(
                session.getId(),
                session.getPlayer() != null ? session.getPlayer().getId() : null,
                session.getPlayer() != null && session.getPlayer().getUser() != null
                        ? session.getPlayer().getUser().getUsername() : null,
                session.getSessionName(),
                session.getStartTime(),
                session.getEndTime(),
                session.getTotalDistanceMeters(),
                session.getAvgSpeedKmh(),
                session.getMaxSpeedKmh(),
                session.getDurationSeconds(),
                session.getPointCount(),
                session.getCreatedAt()
        );
    }
}
