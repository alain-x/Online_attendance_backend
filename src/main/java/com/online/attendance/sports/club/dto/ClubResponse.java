package com.online.attendance.sports.club.dto;

import com.online.attendance.sports.club.SportsClub;

public record ClubResponse(
        Long id,
        String name,
        String slug,
        String logoUrl,
        String description,
        String contactEmail,
        String contactPhone,
        String address,
        boolean active
) {
    public static ClubResponse from(SportsClub club) {
        return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getSlug(),
                club.getLogoUrl(),
                club.getDescription(),
                club.getContactEmail(),
                club.getContactPhone(),
                club.getAddress(),
                club.isActive()
        );
    }
}
