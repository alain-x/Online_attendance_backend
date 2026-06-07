package com.online.attendance.sports.parent;

import com.online.attendance.sports.player.PlayerProfile;
import com.online.attendance.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sports_parent_links", uniqueConstraints = {
        @UniqueConstraint(name = "uk_parent_link", columnNames = {"parent_user_id", "player_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private AppUser parentUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(length = 50)
    private String relationship;

    @Column(name = "created_at")
    private Instant createdAt;
}
