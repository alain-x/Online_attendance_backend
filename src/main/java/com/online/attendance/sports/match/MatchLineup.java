package com.online.attendance.sports.match;

import com.online.attendance.sports.player.PlayerProfile;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sports_match_lineups", uniqueConstraints = {
        @UniqueConstraint(name = "uk_match_lineup", columnNames = {"match_id", "player_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchLineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    @Column(length = 100)
    private String position;

    @Column(name = "is_starter", nullable = false)
    @Builder.Default
    private boolean isStarter = false;

    @Column(name = "substituted_in")
    @Builder.Default
    private boolean substitutedIn = false;

    @Column(name = "substituted_out")
    @Builder.Default
    private boolean substitutedOut = false;

    @Column(name = "minutes_played")
    private Integer minutesPlayed;
}
