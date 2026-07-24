package com.online.attendance.sports.evaluation;

import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.sports.evaluation.dto.AddCriterionRequest;
import com.online.attendance.sports.evaluation.dto.CreateEvaluationRequest;
import com.online.attendance.sports.evaluation.dto.EvaluationResponse;
import com.online.attendance.sports.player.PlayerProfile;
import com.online.attendance.sports.player.PlayerProfileRepository;
import com.online.attendance.sports.team.Team;
import com.online.attendance.sports.team.TeamRepository;
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
@RequestMapping("/api/sports/evaluations")
@Transactional(readOnly = true)
public class EvaluationController {

    private static final Logger log = LoggerFactory.getLogger(EvaluationController.class);
    private final PlayerEvaluationRepository evaluationRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final CurrentCompanyService currentCompanyService;

    public EvaluationController(PlayerEvaluationRepository evaluationRepository,
                                EvaluationCriterionRepository criterionRepository,
                                PlayerProfileRepository playerProfileRepository,
                                TeamRepository teamRepository,
                                UserRepository userRepository,
                                CurrentCompanyService currentCompanyService) {
        this.evaluationRepository = evaluationRepository;
        this.criterionRepository = criterionRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<EvaluationResponse> list(@RequestParam(required = false) Long playerId,
                                          @RequestParam(required = false) Long teamId,
                                          Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        List<PlayerEvaluation> evaluations;
        if (playerId != null) {
            var player = playerProfileRepository.findByIdAndClubCompanyId(playerId, companyId);
            if (player.isEmpty()) {
                return List.of();
            }
            evaluations = evaluationRepository.findByPlayerId(playerId);
        } else if (teamId != null) {
            var team = teamRepository.findByIdAndClubCompanyId(teamId, companyId);
            if (team.isEmpty()) {
                return List.of();
            }
            evaluations = evaluationRepository.findByTeamId(teamId);
        } else {
            evaluations = evaluationRepository.findByClubCompanyId(companyId);
        }
        return evaluations.stream()
                .map(e -> EvaluationResponse.from(e, criterionRepository.findByEvaluationId(e.getId())))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var evaluation = evaluationRepository.findByIdAndClubCompanyId(id, companyId);
        if (evaluation.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Evaluation not found"));
        }
        return ResponseEntity.ok(EvaluationResponse.from(evaluation.get(), criterionRepository.findByEvaluationId(evaluation.get().getId())));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateEvaluationRequest request) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        PlayerProfile player = playerProfileRepository.findByIdAndClubCompanyId(request.getPlayerId(), companyId).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Player not found"));
        }
        Team team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        AppUser evaluator = resolveUser(authentication);
        if (evaluator == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authenticated user not found"));
        }
        Instant now = Instant.now();
        PlayerEvaluation evaluation = PlayerEvaluation.builder()
                .player(player)
                .evaluator(evaluator)
                .team(team)
                .period(request.getPeriod())
                .overallRating(request.getOverallRating())
                .coachNotes(request.getCoachNotes())
                .goals(request.getGoals())
                .avgSpeedKmh(request.getAvgSpeedKmh())
                .maxSpeedKmh(request.getMaxSpeedKmh())
                .totalDistanceKm(request.getTotalDistanceKm())
                .totalTrainingMinutes(request.getTotalTrainingMinutes())
                .createdAt(now)
                .updatedAt(now)
                .build();
        evaluation = evaluationRepository.save(evaluation);
        return ResponseEntity.ok(EvaluationResponse.from(evaluation, List.of()));
    }

    private AppUser resolveUser(Authentication authentication) {
        String principal = authentication.getName();
        if (principal == null) return null;
        int idx = principal.indexOf("::");
        String companySlug = (idx > 0) ? principal.substring(0, idx) : null;
        String username = (idx > 0 && idx + 2 < principal.length()) ? principal.substring(idx + 2) : principal;
        if (companySlug != null) {
            return userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateEvaluationRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var existing = evaluationRepository.findByIdAndClubCompanyId(id, companyId);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Evaluation not found"));
        }
        var evaluation = existing.get();
        Team team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        evaluation.setPeriod(request.getPeriod());
        evaluation.setOverallRating(request.getOverallRating());
        evaluation.setCoachNotes(request.getCoachNotes());
        evaluation.setGoals(request.getGoals());
        evaluation.setAvgSpeedKmh(request.getAvgSpeedKmh());
        evaluation.setMaxSpeedKmh(request.getMaxSpeedKmh());
        evaluation.setTotalDistanceKm(request.getTotalDistanceKm());
        evaluation.setTotalTrainingMinutes(request.getTotalTrainingMinutes());
        evaluation.setTeam(team);
        evaluation.setUpdatedAt(Instant.now());
        evaluation = evaluationRepository.save(evaluation);
        return ResponseEntity.ok().body(EvaluationResponse.from(evaluation, criterionRepository.findByEvaluationId(evaluation.getId())));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var evaluation = evaluationRepository.findByIdAndClubCompanyId(id, companyId);
        if (evaluation.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Evaluation not found"));
        }
        evaluationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/{evaluationId}/criteria")
    @Transactional
    public ResponseEntity<?> addCriterion(@PathVariable Long evaluationId, @Valid @RequestBody AddCriterionRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        PlayerEvaluation evaluation = evaluationRepository.findByIdAndClubCompanyId(evaluationId, companyId).orElse(null);
        if (evaluation == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Evaluation not found"));
        }
        EvaluationCriterion criterion = EvaluationCriterion.builder()
                .evaluation(evaluation)
                .criterionName(request.getCriterionName())
                .score(request.getScore())
                .notes(request.getNotes())
                .build();
        criterion = criterionRepository.save(criterion);
        return ResponseEntity.ok(Map.of(
                "id", criterion.getId(),
                "criterionName", criterion.getCriterionName(),
                "score", criterion.getScore(),
                "notes", criterion.getNotes()
        ));
    }
}
