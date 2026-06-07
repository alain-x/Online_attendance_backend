package com.online.attendance.sports.match.dto;

import com.online.attendance.sports.match.Match;

import java.time.Instant;
import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        Long teamId,
        String teamName,
        String opponent,
        String location,
        LocalDateTime matchDate,
        String type,
        String homeAway,
        String status,
        Integer ourScore,
        Integer opponentScore,
        String notes,
        long lineupCount,
        Instant createdAt
) {
    public static MatchResponse from(Match match, long lineupCount) {
        return new MatchResponse(
                match.getId(),
                match.getTeam() != null ? match.getTeam().getId() : null,
                match.getTeam() != null ? match.getTeam().getName() : null,
                match.getOpponent(),
                match.getLocation(),
                match.getMatchDate(),
                match.getType(),
                match.getHomeAway(),
                match.getStatus(),
                match.getOurScore(),
                match.getOpponentScore(),
                match.getNotes(),
                lineupCount,
                match.getCreatedAt()
        );
    }
}
