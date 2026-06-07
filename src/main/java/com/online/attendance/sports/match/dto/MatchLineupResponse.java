package com.online.attendance.sports.match.dto;

import com.online.attendance.sports.match.MatchLineup;

public record MatchLineupResponse(
        Long id,
        Long matchId,
        Long playerId,
        String playerName,
        Integer jerseyNumber,
        String position,
        boolean isStarter,
        boolean substitutedIn,
        boolean substitutedOut,
        Integer minutesPlayed
) {
    public static MatchLineupResponse from(MatchLineup lineup) {
        return new MatchLineupResponse(
                lineup.getId(),
                lineup.getMatch() != null ? lineup.getMatch().getId() : null,
                lineup.getPlayer() != null ? lineup.getPlayer().getId() : null,
                lineup.getPlayer() != null && lineup.getPlayer().getUser() != null ? lineup.getPlayer().getUser().getUsername() : null,
                lineup.getJerseyNumber(),
                lineup.getPosition(),
                lineup.isStarter(),
                lineup.isSubstitutedIn(),
                lineup.isSubstitutedOut(),
                lineup.getMinutesPlayed()
        );
    }
}
