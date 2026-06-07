package com.online.attendance.sports.team;

import com.online.attendance.sports.player.PlayerProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sports_team_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_team_member", columnNames = {"team_id", "player_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    @Column(length = 100)
    private String position;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
