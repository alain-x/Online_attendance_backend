package com.online.attendance.sports.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerEvaluationRepository extends JpaRepository<PlayerEvaluation, Long> {
    List<PlayerEvaluation> findByPlayerId(Long playerId);
    List<PlayerEvaluation> findByTeamId(Long teamId);
    List<PlayerEvaluation> findByEvaluatorId(Long evaluatorId);
}
