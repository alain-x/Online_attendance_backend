package com.online.attendance.sports.speed;

import com.online.attendance.sports.player.PlayerProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sports_speed_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeedSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "session_name", length = 200)
    private String sessionName;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "total_distance_meters")
    private Double totalDistanceMeters;

    @Column(name = "avg_speed_kmh")
    private Double avgSpeedKmh;

    @Column(name = "max_speed_kmh")
    private Double maxSpeedKmh;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "point_count")
    private Integer pointCount;

    @Column(name = "gps_points", columnDefinition = "TEXT")
    private String gpsPoints;

    @Column(name = "created_at")
    private Instant createdAt;
}
