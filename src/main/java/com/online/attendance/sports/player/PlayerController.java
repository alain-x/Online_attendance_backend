package com.online.attendance.sports.player;

import com.online.attendance.employee.Employee;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.security.CurrentCompanyService;
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
import org.springframework.security.core.Authentication;
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
    private final CurrentCompanyService currentCompanyService;

    public PlayerController(PlayerProfileRepository playerProfileRepository,
                            PlayerStatisticRepository playerStatisticRepository,
                            SportsClubRepository clubRepository,
                            UserRepository userRepository,
                            EmployeeRepository employeeRepository,
                            TeamMemberRepository teamMemberRepository,
                            CurrentCompanyService currentCompanyService) {
        this.playerProfileRepository = playerProfileRepository;
        this.playerStatisticRepository = playerStatisticRepository;
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<PlayerResponse> list(@RequestParam(required = false) Long clubId,
                                     @RequestParam(required = false) Long teamId,
                                     Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        List<PlayerProfile> players;
        if (teamId != null) {
            players = playerProfileRepository.findByClubCompanyId(companyId).stream()
                    .filter(p -> {
                        var memberships = teamMemberRepository.findByTeamId(teamId);
                        return memberships.stream().anyMatch(m -> m.getPlayer().getId().equals(p.getId()));
                    })
                    .collect(Collectors.toList());
        } else if (clubId != null) {
            var club = clubRepository.findByIdAndCompanyId(clubId, companyId);
            if (club.isEmpty()) {
                return List.of();
            }
            players = playerProfileRepository.findByClubId(clubId);
        } else {
            players = playerProfileRepository.findByClubCompanyId(companyId);
        }
        return players.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser user = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        var profile = playerProfileRepository.findByUserId(user.getId());
        if (profile.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Player profile not found for current user"));
        }
        return ResponseEntity.ok(toResponse(profile.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var profile = playerProfileRepository.findByIdAndClubCompanyId(id, companyId);
        if (profile.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Player not found"));
        }
        return ResponseEntity.ok(toResponse(profile.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreatePlayerRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        AppUser user = userRepository.findById(request.getUserId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }
        if (user.getCompany() == null || !user.getCompany().getId().equals(companyId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "User does not belong to your company"));
        }
        if (playerProfileRepository.findByUserId(request.getUserId()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "Player profile already exists for this user"));
        }
        Employee employee = employeeRepository.findByUserIdAndUserCompanyId(request.getUserId(), user.getCompany() != null ? user.getCompany().getId() : null).orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee record not found for this user"));
        }
        SportsClub club = clubRepository.findByIdAndCompanyId(request.getClubId(), companyId).orElse(null);
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

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreatePlayerRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var existing = playerProfileRepository.findByIdAndClubCompanyId(id, companyId);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Player not found"));
        }
        var profile = existing.get();
        SportsClub club = clubRepository.findByIdAndCompanyId(request.getClubId(), companyId).orElse(null);
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

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var player = playerProfileRepository.findByIdAndClubCompanyId(id, companyId);
        if (player.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Player not found"));
        }
        playerProfileRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}/statistics")
    public ResponseEntity<?> getStatistics(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var player = playerProfileRepository.findByIdAndClubCompanyId(id, companyId);
        if (player.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Player not found"));
        }
        return ResponseEntity.ok(playerStatisticRepository.findByPlayerId(id).stream()
                .map(PlayerStatisticResponse::from)
                .collect(Collectors.toList()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/{id}/statistics")
    public ResponseEntity<?> updateStatistics(@PathVariable Long id, @Valid @RequestBody PlayerStatisticResponse request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        PlayerProfile player = playerProfileRepository.findByIdAndClubCompanyId(id, companyId).orElse(null);
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
            firstName = profile.getUser().getFirstName();
            lastName = profile.getUser().getLastName();
        }
        return PlayerResponse.from(profile, email, firstName, lastName);
    }
}
