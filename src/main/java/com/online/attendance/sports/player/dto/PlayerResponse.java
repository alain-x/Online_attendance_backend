package com.online.attendance.sports.player.dto;

import com.online.attendance.sports.player.PlayerProfile;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlayerResponse(
        Long id,
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        Long clubId,
        String clubName,
        LocalDate dateOfBirth,
        BigDecimal height,
        BigDecimal weight,
        String position,
        String dominantHand,
        String medicalNotes,
        String emergencyContact,
        String emergencyPhone,
        String profileImageUrl,
        boolean active
) {
    public static PlayerResponse from(PlayerProfile profile, String email, String firstName, String lastName) {
        return new PlayerResponse(
                profile.getId(),
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getUser() != null ? profile.getUser().getUsername() : null,
                email,
                firstName,
                lastName,
                profile.getClub() != null ? profile.getClub().getId() : null,
                profile.getClub() != null ? profile.getClub().getName() : null,
                profile.getDateOfBirth(),
                profile.getHeight(),
                profile.getWeight(),
                profile.getPosition(),
                profile.getDominantHand(),
                profile.getMedicalNotes(),
                profile.getEmergencyContact(),
                profile.getEmergencyPhone(),
                profile.getProfileImageUrl(),
                profile.isActive()
        );
    }
}
