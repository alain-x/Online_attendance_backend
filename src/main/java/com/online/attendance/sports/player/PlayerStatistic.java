package com.online.attendance.sports.player;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sports_player_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "matches_played")
    @Builder.Default
    private int matchesPlayed = 0;

    @Column(name = "tries_scored")
    @Builder.Default
    private int triesScored = 0;

    @Column
    @Builder.Default
    private int assists = 0;

    @Column(name = "passes_completed")
    @Builder.Default
    private int passesCompleted = 0;

    @Column(name = "tackles_made")
    @Builder.Default
    private int tacklesMade = 0;

    @Column(name = "training_attendance")
    @Builder.Default
    private int trainingAttendance = 0;

    @Column(length = 50)
    private String season;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
