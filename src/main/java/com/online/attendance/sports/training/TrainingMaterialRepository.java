package com.online.attendance.sports.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingMaterialRepository extends JpaRepository<TrainingMaterial, Long> {
    List<TrainingMaterial> findByTeamId(Long teamId);
    List<TrainingMaterial> findByUploadedById(Long uploadedById);
}
