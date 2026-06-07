package com.online.attendance.sports.player;

import com.online.attendance.employee.Employee;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.sports.club.SportsClub;
import com.online.attendance.sports.club.SportsClubRepository;
import com.online.attendance.sports.player.dto.CreatePlayerRequest;
import com.online.attendance.sports.player.dto.PlayerResponse;
import com.online.attendance.sports.player.dto.PlayerStatisticResponse;
import com.online.attendance.sports.team.TeamMemberRepository;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/players")
@Transactional(readOnly = true)
public class PlayerController {

    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerStatisticRepository playerStatisticRepository;
    private final SportsClubRepository clubRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamMemberRepository teamMemberRepository;

    public PlayerController(PlayerProfileRepository playerProfileRepository,
                            PlayerStatisticRepository playerStatisticRepository,
                            SportsClubRepository clubRepository,
                            UserRepository userRepository,
                            EmployeeRepository employeeRepository,
                            TeamMemberRepository teamMemberRepository) {
        this.playerProfileRepository = playerProfileRepository;
        this.playerStatisticRepository = playerStatisticRepository;
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<PlayerResponse> list(@RequestParam(required = false) Long clubId,
                                     @RequestParam(required = false) Long teamId) {
        List<PlayerProfile> players;
        if (teamId != null) {
            players = playerProfileRepository.findByTeamId(teamId);
        } else if (clubId != null) {
            players = playerProfileRepository.findByClubId(clubId);
        } else {
            players = playerProfileRepository.findAll();
        }
        return players.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        var profile = playerProfileRepository.findById(id);
        if (profile.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Player not found"));
        }
        return ResponseEntity.ok(toResponse(profile.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreatePlayerRequest request) {
        AppUser user = userRepository.findById(request.getUserId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }
        if (playerProfileRepository.findByUserId(request.getUserId()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "Player profile already exists for this user"));
        }
        Employee employee = employeeRepository.findByUserIdAndUserCompanyId(request.getUserId(), user.getCompany() != null ? user.getCompany().getId() : null).orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee record not found for this user"));
        }
        SportsClub club = clubRepository.findById(request.getClubId()).orElse(null);
        if (club == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Club not found"));
        }
        PlayerProfile profile = PlayerProfile.builder()
                .user(user)
                .club(club)
                .dateOfBirth(request.getDateOfBirth())
                .height(request.getHeight())
                .weight(request.getWeight())
                .position(request.getPosition())
                .medicalNotes(request.getMedicalNotes())
                .build();
        profile = playerProfileRepository.save(profile);
        return ResponseEntity.ok(toResponse(profile));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreatePlayerRequest request) {
        var existing = playerProfileRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Player not found"));
        }
        var profile = existing.get();
        SportsClub club = clubRepository.findById(request.getClubId()).orElse(null);
        if (club == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Club not found"));
        }
        profile.setClub(club);
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setHeight(request.getHeight());
        profile.setWeight(request.getWeight());
        profile.setPosition(request.getPosition());
        profile.setMedicalNotes(request.getMedicalNotes());
        profile = playerProfileRepository.save(profile);
        return ResponseEntity.ok().body(toResponse(profile));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!playerProfileRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Player not found"));
        }
        playerProfileRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}/statistics")
    public List<PlayerStatisticResponse> getStatistics(@PathVariable Long id) {
        return playerStatisticRepository.findByPlayerId(id).stream()
                .map(PlayerStatisticResponse::from)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/{id}/statistics")
    public ResponseEntity<?> updateStatistics(@PathVariable Long id, @Valid @RequestBody PlayerStatisticResponse request) {
        PlayerProfile player = playerProfileRepository.findById(id).orElse(null);
        if (player == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Player not found"));
        }
        PlayerStatistic stat = PlayerStatistic.builder()
                .player(player)
                .matchesPlayed(request.matchesPlayed())
                .triesScored(request.triesScored())
                .assists(request.assists())
                .passesCompleted(request.passesCompleted())
                .tacklesMade(request.tacklesMade())
                .trainingAttendance(request.trainingAttendance())
                .season(request.season())
                .updatedAt(java.time.Instant.now())
                .build();
        stat = playerStatisticRepository.save(stat);
        return ResponseEntity.ok(PlayerStatisticResponse.from(stat));
    }

    private PlayerResponse toResponse(PlayerProfile profile) {
        String email = null;
        String firstName = null;
        String lastName = null;
        if (profile.getUser() != null) {
            email = profile.getUser().getEmail();
        }
        return PlayerResponse.from(profile, email, firstName, lastName);
    }
}
