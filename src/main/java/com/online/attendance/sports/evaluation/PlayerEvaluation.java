package com.online.attendance.sports.evaluation;

import com.online.attendance.sports.player.PlayerProfile;
import com.online.attendance.sports.team.Team;
import com.online.attendance.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sports_player_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private AppUser evaluator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(length = 50)
    private String period;

    @Column(name = "overall_rating")
    private Integer overallRating;

    @Column(name = "coach_notes", columnDefinition = "TEXT")
    private String coachNotes;

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(name = "avg_speed_kmh")
    private Double avgSpeedKmh;

    @Column(name = "max_speed_kmh")
    private Double maxSpeedKmh;

    @Column(name = "total_distance_km")
    private Double totalDistanceKm;

    @Column(name = "total_training_minutes")
    private Integer totalTrainingMinutes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
