package com.online.attendance.sports.evaluation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sports_evaluation_criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private PlayerEvaluation evaluation;

    @Column(name = "criterion_name", nullable = false, length = 100)
    private String criterionName;

    @Column
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
