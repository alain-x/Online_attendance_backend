package com.online.attendance.sports.team.dto;

import com.online.attendance.sports.team.Team;

public record TeamResponse(
        Long id,
        String name,
        String ageGroup,
        Long sportId,
        String sportName,
        Long clubId,
        String clubName,
        Long coachId,
        String coachName,
        long playerCount,
        boolean active
) {
    public static TeamResponse from(Team team, long playerCount) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getAgeGroup(),
                team.getSport() != null ? team.getSport().getId() : null,
                team.getSport() != null ? team.getSport().getName() : null,
                team.getClub() != null ? team.getClub().getId() : null,
                team.getClub() != null ? team.getClub().getName() : null,
                team.getCoach() != null ? team.getCoach().getId() : null,
                team.getCoach() != null ? team.getCoach().getUsername() : null,
                playerCount,
                team.isActive()
        );
    }
}
