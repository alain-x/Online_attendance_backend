package com.online.attendance.sports.evaluation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = {"player", "player.user", "evaluator", "team"})
    @Query("SELECT pe FROM PlayerEvaluation pe JOIN pe.team t JOIN t.club c WHERE c.company.id = :companyId")
    List<PlayerEvaluation> findByClubCompanyId(@Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"player", "player.user", "evaluator", "team"})
    @Query("SELECT pe FROM PlayerEvaluation pe JOIN pe.team t JOIN t.club c WHERE c.company.id = :companyId AND pe.id = :id")
    Optional<PlayerEvaluation> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
