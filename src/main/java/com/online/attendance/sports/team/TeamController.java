package com.online.attendance.sports.team;

import com.online.attendance.sports.club.SportsClub;
import com.online.attendance.sports.club.SportsClubRepository;
import com.online.attendance.sports.player.PlayerProfile;
import com.online.attendance.sports.player.PlayerProfileRepository;
import com.online.attendance.sports.sport.Sport;
import com.online.attendance.sports.sport.SportRepository;
import com.online.attendance.sports.team.dto.*;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/teams")
@Transactional(readOnly = true)
public class TeamController {

    private static final Logger log = LoggerFactory.getLogger(TeamController.class);
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SportRepository sportRepository;
    private final SportsClubRepository clubRepository;
    private final UserRepository userRepository;
    private final PlayerProfileRepository playerProfileRepository;

    public TeamController(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
                          SportRepository sportRepository, SportsClubRepository clubRepository,
                          UserRepository userRepository, PlayerProfileRepository playerProfileRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.sportRepository = sportRepository;
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<TeamResponse> list(@RequestParam(required = false) Long clubId,
                                   @RequestParam(required = false) Long sportId) {
        List<Team> teams;
        if (clubId != null) {
            teams = teamRepository.findByClubId(clubId);
        } else if (sportId != null) {
            teams = teamRepository.findBySportId(sportId);
        } else {
            teams = teamRepository.findAll();
        }
        return teams.stream()
                .map(team -> TeamResponse.from(team, teamMemberRepository.findByTeamId(team.getId()).size()))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        var team = teamRepository.findById(id);
        if (team.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Team not found"));
        }
        return ResponseEntity.ok(TeamResponse.from(team.get(), teamMemberRepository.findByTeamId(team.get().getId()).size()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreateTeamRequest request) {
        Sport sport = sportRepository.findById(request.getSportId()).orElse(null);
        if (sport == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Sport not found"));
        }
        SportsClub club = clubRepository.findById(request.getClubId()).orElse(null);
        if (club == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Club not found"));
        }
        AppUser coach = null;
        if (request.getCoachId() != null) {
            coach = userRepository.findById(request.getCoachId()).orElse(null);
            if (coach == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coach not found"));
            }
        }
        Team team = Team.builder()
                .name(request.getName())
                .ageGroup(request.getAgeGroup())
                .sport(sport)
                .club(club)
                .coach(coach)
                .description(request.getDescription())
                .build();
        team = teamRepository.save(team);
        return ResponseEntity.ok(TeamResponse.from(team, 0));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateTeamRequest request) {
        var existing = teamRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Team not found"));
        }
        var team = existing.get();
        Sport sport = sportRepository.findById(request.getSportId()).orElse(null);
        if (sport == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Sport not found"));
        }
        SportsClub club = clubRepository.findById(request.getClubId()).orElse(null);
        if (club == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Club not found"));
        }
        AppUser coach = null;
        if (request.getCoachId() != null) {
            coach = userRepository.findById(request.getCoachId()).orElse(null);
        }
        team.setName(request.getName());
        team.setAgeGroup(request.getAgeGroup());
        team.setSport(sport);
        team.setClub(club);
        team.setCoach(coach);
        team.setDescription(request.getDescription());
        team = teamRepository.save(team);
        return ResponseEntity.ok().body(TeamResponse.from(team, teamMemberRepository.findByTeamId(team.getId()).size()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!teamRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Team not found"));
        }
        teamRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/{teamId}/members")
    @Transactional
    public ResponseEntity<?> addMember(@PathVariable Long teamId, @Valid @RequestBody AddTeamMemberRequest request) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Team not found"));
        }
        PlayerProfile player = playerProfileRepository.findById(request.getPlayerId()).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Player not found"));
        }
        boolean alreadyMember = teamMemberRepository.findByTeamId(teamId).stream()
                .anyMatch(m -> m.getPlayer().getId().equals(request.getPlayerId()));
        if (alreadyMember) {
            return ResponseEntity.status(409).body(Map.of("message", "Player is already a member of this team"));
        }
        TeamMember member = TeamMember.builder()
                .team(team)
                .player(player)
                .jerseyNumber(request.getJerseyNumber())
                .position(request.getPosition())
                .joinedDate(LocalDate.now())
                .build();
        member = teamMemberRepository.save(member);
        return ResponseEntity.ok(TeamMemberResponse.from(member));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @DeleteMapping("/{teamId}/members/{memberId}")
    @Transactional
    public ResponseEntity<?> removeMember(@PathVariable Long teamId, @PathVariable Long memberId) {
        TeamMember member = teamMemberRepository.findById(memberId).orElse(null);
        if (member == null || !member.getTeam().getId().equals(teamId)) {
            return ResponseEntity.status(404).body(Map.of("message", "Team member not found"));
        }
        teamMemberRepository.delete(member);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{teamId}/members")
    public List<TeamMemberResponse> listMembers(@PathVariable Long teamId) {
        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(TeamMemberResponse::from)
                .collect(Collectors.toList());
    }
}
