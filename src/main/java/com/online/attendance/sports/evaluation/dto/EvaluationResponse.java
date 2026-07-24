package com.online.attendance.sports.evaluation.dto;

import com.online.attendance.sports.evaluation.EvaluationCriterion;
import com.online.attendance.sports.evaluation.PlayerEvaluation;

import java.time.Instant;
import java.util.List;

public record EvaluationResponse(
        Long id,
        Long playerId,
        String playerName,
        Long evaluatorId,
        String evaluatorName,
        Long teamId,
        String teamName,
        String period,
        Integer overallRating,
        String coachNotes,
        String goals,
        Double avgSpeedKmh,
        Double maxSpeedKmh,
        Double totalDistanceKm,
        Integer totalTrainingMinutes,
        List<CriterionDto> criteria,
        Instant createdAt,
        Instant updatedAt
) {
    public static EvaluationResponse from(PlayerEvaluation evaluation, List<EvaluationCriterion> criteria) {
        List<CriterionDto> criterionDtos = criteria.stream()
                .map(c -> new CriterionDto(c.getId(), c.getCriterionName(), c.getScore(), c.getNotes()))
                .toList();
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getPlayer() != null ? evaluation.getPlayer().getId() : null,
                evaluation.getPlayer() != null && evaluation.getPlayer().getUser() != null ? evaluation.getPlayer().getUser().getUsername() : null,
                evaluation.getEvaluator() != null ? evaluation.getEvaluator().getId() : null,
                evaluation.getEvaluator() != null ? evaluation.getEvaluator().getUsername() : null,
                evaluation.getTeam() != null ? evaluation.getTeam().getId() : null,
                evaluation.getTeam() != null ? evaluation.getTeam().getName() : null,
                evaluation.getPeriod(),
                evaluation.getOverallRating(),
                evaluation.getCoachNotes(),
                evaluation.getGoals(),
                evaluation.getAvgSpeedKmh(),
                evaluation.getMaxSpeedKmh(),
                evaluation.getTotalDistanceKm(),
                evaluation.getTotalTrainingMinutes(),
                criterionDtos,
                evaluation.getCreatedAt(),
                evaluation.getUpdatedAt()
        );
    }

    public record CriterionDto(Long id, String criterionName, Integer score, String notes) {}
}
