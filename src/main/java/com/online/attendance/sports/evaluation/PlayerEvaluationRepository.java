package com.online.attendance.sports.evaluation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerEvaluationRepository extends JpaRepository<PlayerEvaluation, Long> {

    @EntityGraph(attributePaths = {"player", "player.user", "evaluator", "team"})
    @Override
    List<PlayerEvaluation> findAll();

    @EntityGraph(attributePaths = {"player", "player.user", "evaluator", "team"})
    @Override
    Optional<PlayerEvaluation> findById(Long id);

    @EntityGraph(attributePaths = {"player", "player.user", "evaluator", "team"})
    List<PlayerEvaluation> findByPlayerId(Long playerId);

    @EntityGraph(attributePaths = {"player", "player.user", "evaluator", "team"})
    List<PlayerEvaluation> findByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"player", "player.user", "evaluator", "team"})
    List<PlayerEvaluation> findByEvaluatorId(Long evaluatorId);
}
