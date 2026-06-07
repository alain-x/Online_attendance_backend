package com.online.attendance.sports.training.dto;

import com.online.attendance.sports.training.TrainingMaterial;

import java.time.Instant;

public record TrainingMaterialResponse(
        Long id,
        Long teamId,
        String teamName,
        String title,
        String description,
        String fileUrl,
        String fileType,
        Long uploadedById,
        String uploadedByName,
        Instant createdAt
) {
    public static TrainingMaterialResponse from(TrainingMaterial material) {
        return new TrainingMaterialResponse(
                material.getId(),
                material.getTeam() != null ? material.getTeam().getId() : null,
                material.getTeam() != null ? material.getTeam().getName() : null,
                material.getTitle(),
                material.getDescription(),
                material.getFileUrl(),
                material.getFileType(),
                material.getUploadedBy() != null ? material.getUploadedBy().getId() : null,
                material.getUploadedBy() != null ? material.getUploadedBy().getUsername() : null,
                material.getCreatedAt()
        );
    }
}
