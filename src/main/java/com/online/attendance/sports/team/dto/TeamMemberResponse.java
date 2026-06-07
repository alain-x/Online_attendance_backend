package com.online.attendance.sports.team.dto;

import com.online.attendance.sports.team.TeamMember;

public record TeamMemberResponse(
        Long id,
        Long teamId,
        String teamName,
        Long playerId,
        String playerName,
        Integer jerseyNumber,
        String position
) {
    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getId(),
                member.getTeam() != null ? member.getTeam().getId() : null,
                member.getTeam() != null ? member.getTeam().getName() : null,
                member.getPlayer() != null ? member.getPlayer().getId() : null,
                member.getPlayer() != null && member.getPlayer().getUser() != null ? member.getPlayer().getUser().getUsername() : null,
                member.getJerseyNumber(),
                member.getPosition()
        );
    }
}
