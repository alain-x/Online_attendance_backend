package com.online.attendance.sports.training;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingMaterialRepository extends JpaRepository<TrainingMaterial, Long> {

    @EntityGraph(attributePaths = {"team", "uploadedBy"})
    @Override
    List<TrainingMaterial> findAll();

    @EntityGraph(attributePaths = {"team", "uploadedBy"})
    List<TrainingMaterial> findByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team", "uploadedBy"})
    List<TrainingMaterial> findByUploadedById(Long uploadedById);
}
