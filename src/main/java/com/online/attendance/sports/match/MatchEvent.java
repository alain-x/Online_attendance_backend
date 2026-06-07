package com.online.attendance.sports.match;

import com.online.attendance.sports.player.PlayerProfile;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sports_match_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private PlayerProfile player;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column
    private Integer minute;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
