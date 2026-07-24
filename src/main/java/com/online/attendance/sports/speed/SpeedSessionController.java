package com.online.attendance.sports.speed;

import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.sports.speed.dto.CreateSpeedSessionRequest;
import com.online.attendance.sports.speed.dto.SpeedSessionResponse;
import com.online.attendance.sports.player.PlayerProfile;
import com.online.attendance.sports.player.PlayerProfileRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/speed")
@Transactional(readOnly = true)
public class SpeedSessionController {

    private final SpeedSessionRepository repository;
    private final PlayerProfileRepository playerProfileRepository;
    private final CurrentCompanyService currentCompanyService;

    public SpeedSessionController(SpeedSessionRepository repository,
                                  PlayerProfileRepository playerProfileRepository,
                                  CurrentCompanyService currentCompanyService) {
        this.repository = repository;
        this.playerProfileRepository = playerProfileRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER')")
    @GetMapping("/sessions")
    public List<SpeedSessionResponse> listSessions(@RequestParam(required = false) Long playerId,
                                                    Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        List<SpeedSession> sessions;
        if (playerId != null) {
            var player = playerProfileRepository.findByIdAndClubCompanyId(playerId, companyId);
            if (player.isEmpty()) {
                return List.of();
            }
            sessions = repository.findByPlayerIdOrderByCreatedAtDesc(playerId);
        } else {
            sessions = repository.findByClubCompanyId(companyId);
        }
        return sessions.stream().map(SpeedSessionResponse::from).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER')")
    @GetMapping("/sessions/{id}")
    public ResponseEntity<?> getSessionById(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var session = repository.findByIdAndClubCompanyId(id, companyId);
        if (session.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Speed session not found"));
        }
        return ResponseEntity.ok(SpeedSessionResponse.from(session.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER')")
    @PostMapping("/sessions")
    @Transactional
    public ResponseEntity<?> createSession(@Valid @RequestBody CreateSpeedSessionRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        PlayerProfile player = playerProfileRepository.findByIdAndClubCompanyId(request.getPlayerId(), companyId).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Player not found"));
        }
        SpeedSession session = SpeedSession.builder()
                .player(player)
                .sessionName(request.getSessionName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalDistanceMeters(request.getTotalDistanceMeters())
                .avgSpeedKmh(request.getAvgSpeedKmh())
                .maxSpeedKmh(request.getMaxSpeedKmh())
                .durationSeconds(request.getDurationSeconds())
                .pointCount(request.getPointCount())
                .gpsPoints(request.getGpsPoints())
                .createdAt(Instant.now())
                .build();
        session = repository.save(session);
        return ResponseEntity.ok(SpeedSessionResponse.from(session));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER')")
    @DeleteMapping("/sessions/{id}")
    @Transactional
    public ResponseEntity<?> deleteSession(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var session = repository.findByIdAndClubCompanyId(id, companyId);
        if (session.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Speed session not found"));
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER')")
    @GetMapping("/player/{playerId}/stats")
    public ResponseEntity<?> getPlayerStats(@PathVariable Long playerId, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var player = playerProfileRepository.findByIdAndClubCompanyId(playerId, companyId);
        if (player.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Player not found"));
        }
        List<SpeedSession> sessions = repository.findByPlayerIdOrderByCreatedAtDesc(playerId);
        if (sessions.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "totalSessions", 0,
                    "totalDistanceMeters", 0.0,
                    "avgSpeedKmh", 0.0,
                    "maxSpeedKmh", 0.0,
                    "totalDurationSeconds", 0
            ));
        }
        double totalDist = sessions.stream().mapToDouble(s -> s.getTotalDistanceMeters() != null ? s.getTotalDistanceMeters() : 0).sum();
        double avgSpd = sessions.stream().mapToDouble(s -> s.getAvgSpeedKmh() != null ? s.getAvgSpeedKmh() : 0).average().orElse(0);
        double maxSpd = sessions.stream().mapToDouble(s -> s.getMaxSpeedKmh() != null ? s.getMaxSpeedKmh() : 0).max().orElse(0);
        int totalDur = sessions.stream().mapToInt(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0).sum();
        return ResponseEntity.ok(Map.of(
                "totalSessions", sessions.size(),
                "totalDistanceMeters", totalDist,
                "avgSpeedKmh", avgSpd,
                "maxSpeedKmh", maxSpd,
                "totalDurationSeconds", totalDur
        ));
    }
}
