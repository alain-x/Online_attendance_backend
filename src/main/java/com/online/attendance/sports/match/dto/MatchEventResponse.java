package com.online.attendance.sports.match.dto;

import com.online.attendance.sports.match.MatchEvent;

public record MatchEventResponse(
        Long id,
        Long matchId,
        Long playerId,
        String playerName,
        String eventType,
        Integer minute,
        String notes
) {
    public static MatchEventResponse from(MatchEvent event) {
        return new MatchEventResponse(
                event.getId(),
                event.getMatch() != null ? event.getMatch().getId() : null,
                event.getPlayer() != null ? event.getPlayer().getId() : null,
                event.getPlayer() != null && event.getPlayer().getUser() != null ? event.getPlayer().getUser().getUsername() : null,
                event.getEventType(),
                event.getMinute(),
                event.getNotes()
        );
    }
}
