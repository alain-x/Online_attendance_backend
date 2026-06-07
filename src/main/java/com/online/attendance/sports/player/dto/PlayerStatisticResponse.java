package com.online.attendance.sports.player.dto;

import com.online.attendance.sports.player.PlayerStatistic;

import java.time.Instant;

public record PlayerStatisticResponse(
        Long id,
        Long playerId,
        int matchesPlayed,
        int triesScored,
        int assists,
        int passesCompleted,
        int tacklesMade,
        int trainingAttendance,
        String season,
        Instant updatedAt
) {
    public static PlayerStatisticResponse from(PlayerStatistic stat) {
        return new PlayerStatisticResponse(
                stat.getId(),
                stat.getPlayer() != null ? stat.getPlayer().getId() : null,
                stat.getMatchesPlayed(),
                stat.getTriesScored(),
                stat.getAssists(),
                stat.getPassesCompleted(),
                stat.getTacklesMade(),
                stat.getTrainingAttendance(),
                stat.getSeason(),
                stat.getUpdatedAt()
        );
    }
}
