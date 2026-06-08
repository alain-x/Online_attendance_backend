package com.online.attendance.sports.analytics;

import com.online.attendance.sports.match.MatchEventRepository;
import com.online.attendance.sports.match.MatchRepository;
import com.online.attendance.sports.payment.PlayerPaymentRepository;
import com.online.attendance.sports.player.PlayerProfileRepository;
import com.online.attendance.sports.player.PlayerStatisticRepository;
import com.online.attendance.sports.team.Team;
import com.online.attendance.sports.team.TeamRepository;
import com.online.attendance.sports.training.TrainingAttendanceRepository;
import com.online.attendance.sports.training.TrainingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sports/analytics")
@Transactional(readOnly = true)
public class SportsAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(SportsAnalyticsController.class);
    private final PlayerProfileRepository playerProfileRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingAttendanceRepository trainingAttendanceRepository;
    private final PlayerStatisticRepository playerStatisticRepository;
    private final PlayerPaymentRepository playerPaymentRepository;

    public SportsAnalyticsController(PlayerProfileRepository playerProfileRepository,
                               TeamRepository teamRepository,
                               MatchRepository matchRepository,
                               MatchEventRepository matchEventRepository,
                               TrainingSessionRepository trainingSessionRepository,
                               TrainingAttendanceRepository trainingAttendanceRepository,
                               PlayerStatisticRepository playerStatisticRepository,
                               PlayerPaymentRepository playerPaymentRepository) {
        this.playerProfileRepository = playerProfileRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.matchEventRepository = matchEventRepository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.trainingAttendanceRepository = trainingAttendanceRepository;
        this.playerStatisticRepository = playerStatisticRepository;
        this.playerPaymentRepository = playerPaymentRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestParam(required = false) Long clubId) {
        Map<String, Object> data = new HashMap<>();

        long totalPlayers = clubId != null
                ? playerProfileRepository.findByClubId(clubId).size()
                : playerProfileRepository.count();
        data.put("totalPlayers", totalPlayers);

        long totalTeams = clubId != null
                ? teamRepository.findByClubId(clubId).size()
                : teamRepository.count();
        data.put("totalTeams", totalTeams);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekEnd = now.plusWeeks(1);

        long upcomingMatches;
        if (clubId != null) {
            var teams = teamRepository.findByClubId(clubId);
            upcomingMatches = teams.stream()
                    .mapToLong(t -> matchRepository.findByTeamIdAndMatchDateBetween(t.getId(), now, weekEnd).size())
                    .sum();
        } else {
            upcomingMatches = 0;
        }
        data.put("upcomingMatches", upcomingMatches);

        long upcomingTraining;
        if (clubId != null) {
            var teams = teamRepository.findByClubId(clubId);
            upcomingTraining = teams.stream()
                    .mapToLong(t -> trainingSessionRepository.findByTeamIdAndStartTimeBetween(t.getId(), now, weekEnd).size())
                    .sum();
        } else {
            upcomingTraining = 0;
        }
        data.put("upcomingTrainingSessions", upcomingTraining);

        long recentPayments = playerPaymentRepository.findAll().size();
        data.put("recentPayments", recentPayments);

        return data;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/player/{playerId}")
    public Map<String, Object> playerStats(@PathVariable Long playerId) {
        Map<String, Object> data = new HashMap<>();

        var stats = playerStatisticRepository.findByPlayerId(playerId);
        if (!stats.isEmpty()) {
            var s = stats.get(stats.size() - 1);
            data.put("matchesPlayed", s.getMatchesPlayed());
            data.put("triesScored", s.getTriesScored());
            data.put("assists", s.getAssists());
            data.put("passesCompleted", s.getPassesCompleted());
            data.put("tacklesMade", s.getTacklesMade());
            data.put("trainingAttendance", s.getTrainingAttendance());
            data.put("season", s.getSeason());
        }

        var payments = playerPaymentRepository.findByPlayerId(playerId);
        data.put("totalPayments", payments.size());
        long paidCount = payments.stream().filter(p -> "PAID".equals(p.getStatus())).count();
        data.put("paidPayments", paidCount);
        long pendingCount = payments.stream().filter(p -> "PENDING".equals(p.getStatus())).count();
        data.put("pendingPayments", pendingCount);

        return data;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @GetMapping("/team/{teamId}")
    public Map<String, Object> teamStats(@PathVariable Long teamId) {
        Map<String, Object> data = new HashMap<>();

        var matches = matchRepository.findByTeamId(teamId);
        data.put("matchesPlayed", matches.size());

        long wins = matches.stream()
                .filter(m -> m.getOurScore() != null && m.getOpponentScore() != null
                        && m.getOurScore() > m.getOpponentScore())
                .count();
        long losses = matches.stream()
                .filter(m -> m.getOurScore() != null && m.getOpponentScore() != null
                        && m.getOurScore() < m.getOpponentScore())
                .count();
        long draws = matches.stream()
                .filter(m -> m.getOurScore() != null && m.getOpponentScore() != null
                        && m.getOurScore().intValue() == m.getOpponentScore().intValue())
                .count();
        data.put("wins", wins);
        data.put("losses", losses);
        data.put("draws", draws);

        var trainings = trainingSessionRepository.findByTeamId(teamId);
        data.put("totalTrainingSessions", trainings.size());

        int totalAttendance = trainings.stream()
                .mapToInt(t -> trainingAttendanceRepository.findBySessionId(t.getId()).size())
                .sum();
        data.put("totalAttendanceRecords", totalAttendance);

        var payments = playerPaymentRepository.findAll();
        data.put("teamPayments", payments.stream()
                .filter(p -> p.getFee() != null && p.getFee().getTeam() != null
                        && p.getFee().getTeam().getId().equals(teamId))
                .count());

        return data;
    }
}
