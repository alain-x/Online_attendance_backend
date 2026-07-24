package com.online.attendance.sports.match;

import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.sports.match.dto.*;
import com.online.attendance.sports.player.PlayerProfile;
import com.online.attendance.sports.player.PlayerProfileRepository;
import com.online.attendance.sports.team.Team;
import com.online.attendance.sports.team.TeamRepository;
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
@RequestMapping("/api/sports/matches")
@Transactional(readOnly = true)
public class MatchController {

    private static final Logger log = LoggerFactory.getLogger(MatchController.class);
    private final MatchRepository matchRepository;
    private final MatchLineupRepository lineupRepository;
    private final MatchEventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final CurrentCompanyService currentCompanyService;

    public MatchController(MatchRepository matchRepository, MatchLineupRepository lineupRepository,
                           MatchEventRepository eventRepository, TeamRepository teamRepository,
                           PlayerProfileRepository playerProfileRepository,
                           CurrentCompanyService currentCompanyService) {
        this.matchRepository = matchRepository;
        this.lineupRepository = lineupRepository;
        this.eventRepository = eventRepository;
        this.teamRepository = teamRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<MatchResponse> list(@RequestParam(required = false) Long teamId, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        List<Match> matches;
        if (teamId != null) {
            var team = teamRepository.findByIdAndClubCompanyId(teamId, companyId);
            if (team.isEmpty()) {
                return List.of();
            }
            matches = matchRepository.findByTeamId(teamId);
        } else {
            matches = matchRepository.findByClubCompanyId(companyId);
        }
        return matches.stream()
                .map(m -> MatchResponse.from(m, lineupRepository.findByMatchId(m.getId()).size()))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var match = matchRepository.findByIdAndClubCompanyId(id, companyId);
        if (match.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Match not found"));
        }
        return ResponseEntity.ok(MatchResponse.from(match.get(), lineupRepository.findByMatchId(match.get().getId()).size()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreateMatchRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        Team team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        Match match = Match.builder()
                .team(team)
                .opponent(request.getOpponent())
                .location(request.getLocation())
                .matchDate(request.getMatchDate())
                .type(request.getType())
                .homeAway(request.getHomeAway())
                .createdAt(Instant.now())
                .build();
        match = matchRepository.save(match);
        return ResponseEntity.ok(MatchResponse.from(match, 0));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateMatchRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var existing = matchRepository.findByIdAndClubCompanyId(id, companyId);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Match not found"));
        }
        var match = existing.get();
        Team team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        match.setTeam(team);
        match.setOpponent(request.getOpponent());
        match.setLocation(request.getLocation());
        match.setMatchDate(request.getMatchDate());
        match.setType(request.getType());
        match.setHomeAway(request.getHomeAway());
        match = matchRepository.save(match);
        return ResponseEntity.ok().body(MatchResponse.from(match, lineupRepository.findByMatchId(match.getId()).size()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var match = matchRepository.findByIdAndClubCompanyId(id, companyId);
        if (match.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Match not found"));
        }
        matchRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/{matchId}/lineup")
    @Transactional
    public ResponseEntity<?> addToLineup(@PathVariable Long matchId, @Valid @RequestBody AddLineupRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        Match match = matchRepository.findByIdAndClubCompanyId(matchId, companyId).orElse(null);
        if (match == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Match not found"));
        }
        PlayerProfile player = playerProfileRepository.findByIdAndClubCompanyId(request.getPlayerId(), companyId).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Player not found"));
        }
        boolean alreadyInLineup = lineupRepository.findByMatchId(matchId).stream()
                .anyMatch(l -> l.getPlayer().getId().equals(request.getPlayerId()));
        if (alreadyInLineup) {
            return ResponseEntity.status(409).body(Map.of("message", "Player already in lineup"));
        }
        MatchLineup lineup = MatchLineup.builder()
                .match(match)
                .player(player)
                .jerseyNumber(request.getJerseyNumber())
                .position(request.getPosition())
                .isStarter(request.isStarter())
                .build();
        lineup = lineupRepository.save(lineup);
        return ResponseEntity.ok(MatchLineupResponse.from(lineup));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{matchId}/lineup")
    public ResponseEntity<?> getLineup(@PathVariable Long matchId, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var match = matchRepository.findByIdAndClubCompanyId(matchId, companyId);
        if (match.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Match not found"));
        }
        return ResponseEntity.ok(lineupRepository.findByMatchId(matchId).stream()
                .map(MatchLineupResponse::from)
                .collect(Collectors.toList()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @DeleteMapping("/{matchId}/lineup/{lineupId}")
    @Transactional
    public ResponseEntity<?> removeFromLineup(@PathVariable Long matchId, @PathVariable Long lineupId, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var match = matchRepository.findByIdAndClubCompanyId(matchId, companyId);
        if (match.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Match not found"));
        }
        MatchLineup lineup = lineupRepository.findById(lineupId).orElse(null);
        if (lineup == null || !lineup.getMatch().getId().equals(matchId)) {
            return ResponseEntity.status(404).body(Map.of("message", "Lineup entry not found"));
        }
        lineupRepository.delete(lineup);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/{matchId}/events")
    @Transactional
    public ResponseEntity<?> addEvent(@PathVariable Long matchId, @Valid @RequestBody AddMatchEventRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        Match match = matchRepository.findByIdAndClubCompanyId(matchId, companyId).orElse(null);
        if (match == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Match not found"));
        }
        PlayerProfile player = null;
        if (request.getPlayerId() != null) {
            player = playerProfileRepository.findByIdAndClubCompanyId(request.getPlayerId(), companyId).orElse(null);
        }
        MatchEvent event = MatchEvent.builder()
                .match(match)
                .player(player)
                .eventType(request.getEventType())
                .minute(request.getMinute())
                .notes(request.getNotes())
                .build();
        event = eventRepository.save(event);
        return ResponseEntity.ok(MatchEventResponse.from(event));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{matchId}/events")
    public ResponseEntity<?> getEvents(@PathVariable Long matchId, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var match = matchRepository.findByIdAndClubCompanyId(matchId, companyId);
        if (match.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Match not found"));
        }
        return ResponseEntity.ok(eventRepository.findByMatchId(matchId).stream()
                .map(MatchEventResponse::from)
                .collect(Collectors.toList()));
    }
}
