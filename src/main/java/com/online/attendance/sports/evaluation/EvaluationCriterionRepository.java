package com.online.attendance.sports.evaluation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {

    @EntityGraph(attributePaths = {"evaluation"})
    @Override
    List<EvaluationCriterion> findAll();

    @EntityGraph(attributePaths = {"evaluation"})
    List<EvaluationCriterion> findByEvaluationId(Long evaluationId);
}
