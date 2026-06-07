package com.online.attendance.sports.training;

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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/training")
public class TrainingController {

    private static final Logger log = LoggerFactory.getLogger(TrainingController.class);
    private final TrainingSessionRepository sessionRepository;
    private final TrainingAttendanceRepository attendanceRepository;
    private final TrainingMaterialRepository materialRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PlayerProfileRepository playerProfileRepository;

    public TrainingController(TrainingSessionRepository sessionRepository,
                              TrainingAttendanceRepository attendanceRepository,
                              TrainingMaterialRepository materialRepository,
                              TeamRepository teamRepository,
                              UserRepository userRepository,
                              PlayerProfileRepository playerProfileRepository) {
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.materialRepository = materialRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/sessions")
    public List<TrainingSessionResponse> listSessions(@RequestParam(required = false) Long teamId,
                                                       @RequestParam(required = false) Long coachId) {
        List<TrainingSession> sessions;
        if (teamId != null) {
            sessions = sessionRepository.findByTeamId(teamId);
        } else if (coachId != null) {
            sessions = sessionRepository.findByCoachId(coachId);
        } else {
            sessions = sessionRepository.findAll();
        }
        return sessions.stream()
                .map(s -> TrainingSessionResponse.from(s, attendanceRepository.findBySessionId(s.getId()).size()))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/sessions/{id}")
    public ResponseEntity<?> getSessionById(@PathVariable Long id) {
        var session = sessionRepository.findById(id);
        if (session.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        return ResponseEntity.ok(TrainingSessionResponse.from(session.get(), attendanceRepository.findBySessionId(session.get().getId()).size()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@Valid @RequestBody CreateTrainingSessionRequest request) {
        Team team = teamRepository.findById(request.getTeamId()).orElse(null);
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
    public ResponseEntity<?> updateSession(@PathVariable Long id, @Valid @RequestBody CreateTrainingSessionRequest request) {
        var existing = sessionRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        var session = existing.get();
        Team team = teamRepository.findById(request.getTeamId()).orElse(null);
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
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        if (!sessionRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        sessionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<?> markAttendance(@PathVariable Long sessionId, @Valid @RequestBody MarkAttendanceRequest request) {
        TrainingSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Training session not found"));
        }
        PlayerProfile player = playerProfileRepository.findById(request.getPlayerId()).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Player not found"));
        }
        boolean exists = attendanceRepository.findBySessionId(sessionId).stream()
                .anyMatch(a -> a.getPlayer().getId().equals(request.getPlayerId()));
        if (exists) {
            return ResponseEntity.status(409).body(Map.of("message", "Attendance already marked for this player"));
        }
        TrainingAttendance attendance = TrainingAttendance.builder()
                .session(session)
                .player(player)
                .status(request.getStatus())
                .notes(request.getNotes())
                .build();
        attendance = attendanceRepository.save(attendance);
        return ResponseEntity.ok(TrainingAttendanceResponse.from(attendance));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/sessions/{sessionId}/attendance")
    public List<TrainingAttendanceResponse> getAttendance(@PathVariable Long sessionId) {
        return attendanceRepository.findBySessionId(sessionId).stream()
                .map(TrainingAttendanceResponse::from)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/materials")
    public List<TrainingMaterialResponse> listMaterials(@RequestParam(required = false) Long teamId) {
        List<TrainingMaterial> materials;
        if (teamId != null) {
            materials = materialRepository.findByTeamId(teamId);
        } else {
            materials = materialRepository.findAll();
        }
        return materials.stream().map(TrainingMaterialResponse::from).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/materials")
    public ResponseEntity<?> createMaterial(@Valid @RequestBody CreateTrainingMaterialRequest request) {
        Team team = teamRepository.findById(request.getTeamId()).orElse(null);
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
    public ResponseEntity<?> deleteMaterial(@PathVariable Long id) {
        if (!materialRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Material not found"));
        }
        materialRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
