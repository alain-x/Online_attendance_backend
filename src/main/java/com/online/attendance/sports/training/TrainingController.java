package com.online.attendance.sports.training;

import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.sports.player.PlayerProfile;
import com.online.attendance.sports.player.PlayerProfileRepository;
import com.online.attendance.sports.team.Team;
import com.online.attendance.sports.team.TeamRepository;
import com.online.attendance.sports.training.dto.*;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/sports/training")
@Transactional(readOnly = true)
public class TrainingController {

    private static final Logger log = LoggerFactory.getLogger(TrainingController.class);
    private final TrainingSessionRepository sessionRepository;
    private final TrainingAttendanceRepository attendanceRepository;
    private final TrainingMaterialRepository materialRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final CurrentCompanyService currentCompanyService;

    public TrainingController(TrainingSessionRepository sessionRepository,
                              TrainingAttendanceRepository attendanceRepository,
                              TrainingMaterialRepository materialRepository,
                              TeamRepository teamRepository,
                              UserRepository userRepository,
                              PlayerProfileRepository playerProfileRepository,
                              CurrentCompanyService currentCompanyService) {
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.materialRepository = materialRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/sessions")
    public List<TrainingSessionResponse> listSessions(@RequestParam(required = false) Long teamId,
                                                       @RequestParam(required = false) Long coachId,
                                                       Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        List<TrainingSession> sessions;
        if (teamId != null) {
            var team = teamRepository.findByIdAndClubCompanyId(teamId, companyId);
            if (team.isEmpty()) {
                return List.of();
            }
            sessions = sessionRepository.findByTeamId(teamId);
        } else if (coachId != null) {
            sessions = sessionRepository.findByClubCompanyId(companyId).stream()
                    .filter(s -> s.getCoach() != null && s.getCoach().getId().equals(coachId))
                    .collect(Collectors.toList());
        } else {
            sessions = sessionRepository.findByClubCompanyId(companyId);
        }
        return sessions.stream()
                .map(s -> TrainingSessionResponse.from(s, attendanceRepository.findBySessionId(s.getId()).size()))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/sessions/{id}")
    public ResponseEntity<?> getSessionById(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var session = sessionRepository.findByIdAndClubCompanyId(id, companyId);
        if (session.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        return ResponseEntity.ok(TrainingSessionResponse.from(session.get(), attendanceRepository.findBySessionId(session.get().getId()).size()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/sessions")
    @Transactional
    public ResponseEntity<?> createSession(@Valid @RequestBody CreateTrainingSessionRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        Team team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        AppUser coach = null;
        if (request.getCoachId() != null) {
            coach = userRepository.findById(request.getCoachId()).orElse(null);
        }
        TrainingSession session = TrainingSession.builder()
                .team(team)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .coach(coach)
                .build();
        session = sessionRepository.save(session);
        return ResponseEntity.ok(TrainingSessionResponse.from(session, 0));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PutMapping("/sessions/{id}")
    @Transactional
    public ResponseEntity<?> updateSession(@PathVariable Long id, @Valid @RequestBody CreateTrainingSessionRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var existing = sessionRepository.findByIdAndClubCompanyId(id, companyId);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        var session = existing.get();
        Team team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        AppUser coach = null;
        if (request.getCoachId() != null) {
            coach = userRepository.findById(request.getCoachId()).orElse(null);
        }
        session.setTeam(team);
        session.setTitle(request.getTitle());
        session.setDescription(request.getDescription());
        session.setLocation(request.getLocation());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setCoach(coach);
        session = sessionRepository.save(session);
        return ResponseEntity.ok().body(TrainingSessionResponse.from(session, attendanceRepository.findBySessionId(session.getId()).size()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @DeleteMapping("/sessions/{id}")
    @Transactional
    public ResponseEntity<?> deleteSession(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var session = sessionRepository.findByIdAndClubCompanyId(id, companyId);
        if (session.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        sessionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/sessions/{sessionId}/attendance")
    @Transactional
    public ResponseEntity<?> markAttendance(@PathVariable Long sessionId, @Valid @RequestBody List<MarkAttendanceRequest> requests, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        TrainingSession session = sessionRepository.findByIdAndClubCompanyId(sessionId, companyId).orElse(null);
        if (session == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        List<TrainingAttendance> saved = new java.util.ArrayList<>();
        for (MarkAttendanceRequest req : requests) {
            PlayerProfile player = playerProfileRepository.findByIdAndClubCompanyId(req.getPlayerId(), companyId).orElse(null);
            if (player == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Player not found: " + req.getPlayerId()));
            }
            boolean exists = attendanceRepository.findBySessionId(sessionId).stream()
                    .anyMatch(a -> a.getPlayer().getId().equals(req.getPlayerId()));
            if (exists) continue;
            TrainingAttendance attendance = TrainingAttendance.builder()
                    .session(session)
                    .player(player)
                    .status(req.getStatus())
                    .notes(req.getNotes())
                    .build();
            saved.add(attendanceRepository.save(attendance));
        }
        return ResponseEntity.ok(saved.stream().map(TrainingAttendanceResponse::from).collect(Collectors.toList()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<?> getAttendance(@PathVariable Long sessionId, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var session = sessionRepository.findByIdAndClubCompanyId(sessionId, companyId);
        if (session.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        return ResponseEntity.ok(attendanceRepository.findBySessionId(sessionId).stream()
                .map(TrainingAttendanceResponse::from)
                .collect(Collectors.toList()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER')")
    @PostMapping("/sessions/{sessionId}/check-in")
    @Transactional
    public ResponseEntity<?> checkIn(@PathVariable Long sessionId, @RequestBody(required = false) Map<String, Object> body, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        TrainingSession session = sessionRepository.findByIdAndClubCompanyId(sessionId, companyId).orElse(null);
        if (session == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        Long playerId = body != null && body.get("playerId") != null ? ((Number) body.get("playerId")).longValue() : null;
        if (playerId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "playerId is required"));
        }
        PlayerProfile player = playerProfileRepository.findByIdAndClubCompanyId(playerId, companyId).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Player not found"));
        }
        TrainingAttendance attendance = attendanceRepository.findBySessionId(sessionId).stream()
                .filter(a -> a.getPlayer().getId().equals(playerId))
                .findFirst().orElse(null);
        String reason = body != null && body.get("reason") != null ? String.valueOf(body.get("reason")) : null;
        if (attendance == null) {
            attendance = TrainingAttendance.builder()
                    .session(session)
                    .player(player)
                    .status("PRESENT")
                    .checkinReason(reason)
                    .checkedInAt(Instant.now())
                    .build();
        } else {
            attendance.setCheckedInAt(Instant.now());
            attendance.setCheckinReason(reason);
            if ("ABSENT".equals(attendance.getStatus())) {
                attendance.setStatus("PRESENT");
            }
        }
        attendance = attendanceRepository.save(attendance);
        return ResponseEntity.ok(TrainingAttendanceResponse.from(attendance));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER')")
    @PutMapping("/sessions/{sessionId}/check-out")
    @Transactional
    public ResponseEntity<?> checkOut(@PathVariable Long sessionId, @RequestBody(required = false) Map<String, Object> body, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        TrainingSession session = sessionRepository.findByIdAndClubCompanyId(sessionId, companyId).orElse(null);
        if (session == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        Long playerId = body != null && body.get("playerId") != null ? ((Number) body.get("playerId")).longValue() : null;
        if (playerId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "playerId is required"));
        }
        TrainingAttendance attendance = attendanceRepository.findBySessionId(sessionId).stream()
                .filter(a -> a.getPlayer().getId().equals(playerId))
                .findFirst().orElse(null);
        if (attendance == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No check-in record found. Please check in first."));
        }
        if (attendance.getCheckedInAt() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No check-in time recorded. Please check in first."));
        }
        String reason = body != null && body.get("reason") != null ? String.valueOf(body.get("reason")) : null;
        attendance.setCheckedOutAt(Instant.now());
        attendance.setCheckoutReason(reason);
        attendance = attendanceRepository.save(attendance);
        return ResponseEntity.ok(TrainingAttendanceResponse.from(attendance));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/materials")
    public ResponseEntity<?> listMaterials(@RequestParam(required = false) Long teamId, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        if (teamId != null) {
            var team = teamRepository.findByIdAndClubCompanyId(teamId, companyId);
            if (team.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
        }
        List<TrainingMaterial> materials;
        if (teamId != null) {
            materials = materialRepository.findByTeamId(teamId);
        } else {
            var companyTeams = teamRepository.findByClubCompanyId(companyId);
            var teamIds = companyTeams.stream().map(Team::getId).collect(Collectors.toSet());
            materials = materialRepository.findAll().stream()
                    .filter(m -> teamIds.contains(m.getTeam().getId()))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(materials.stream().map(TrainingMaterialResponse::from).collect(Collectors.toList()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/materials")
    public ResponseEntity<?> createMaterial(@Valid @RequestBody CreateTrainingMaterialRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        Team team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        TrainingMaterial material = TrainingMaterial.builder()
                .team(team)
                .title(request.getTitle())
                .description(request.getDescription())
                .fileUrl(request.getFileUrl())
                .fileType(request.getFileType())
                .createdAt(Instant.now())
                .build();
        material = materialRepository.save(material);
        return ResponseEntity.ok(TrainingMaterialResponse.from(material));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @DeleteMapping("/materials/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var material = materialRepository.findById(id);
        if (material.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Material not found"));
        }
        var team = teamRepository.findByIdAndClubCompanyId(material.get().getTeam().getId(), companyId);
        if (team.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Material not found"));
        }
        materialRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
