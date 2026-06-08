package com.online.attendance.sports.evaluation;

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

    public EvaluationController(PlayerEvaluationRepository evaluationRepository,
                                EvaluationCriterionRepository criterionRepository,
                                PlayerProfileRepository playerProfileRepository,
                                TeamRepository teamRepository,
                                UserRepository userRepository) {
        this.evaluationRepository = evaluationRepository;
        this.criterionRepository = criterionRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<EvaluationResponse> list(@RequestParam(required = false) Long playerId,
                                          @RequestParam(required = false) Long teamId) {
        List<PlayerEvaluation> evaluations;
        if (playerId != null) {
            evaluations = evaluationRepository.findByPlayerId(playerId);
        } else if (teamId != null) {
            evaluations = evaluationRepository.findByTeamId(teamId);
        } else {
            evaluations = evaluationRepository.findAll();
        }
        return evaluations.stream()
                .map(e -> EvaluationResponse.from(e, criterionRepository.findByEvaluationId(e.getId())))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        var evaluation = evaluationRepository.findById(id);
        if (evaluation.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Evaluation not found"));
        }
        return ResponseEntity.ok(EvaluationResponse.from(evaluation.get(), criterionRepository.findByEvaluationId(evaluation.get().getId())));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateEvaluationRequest request) {
        PlayerProfile player = playerProfileRepository.findById(request.getPlayerId()).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Player not found"));
        }
        Team team = teamRepository.findById(request.getTeamId()).orElse(null);
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
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateEvaluationRequest request) {
        var existing = evaluationRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Evaluation not found"));
        }
        var evaluation = existing.get();
        Team team = teamRepository.findById(request.getTeamId()).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        evaluation.setPeriod(request.getPeriod());
        evaluation.setOverallRating(request.getOverallRating());
        evaluation.setCoachNotes(request.getCoachNotes());
        evaluation.setGoals(request.getGoals());
        evaluation.setTeam(team);
        evaluation.setUpdatedAt(Instant.now());
        evaluation = evaluationRepository.save(evaluation);
        return ResponseEntity.ok().body(EvaluationResponse.from(evaluation, criterionRepository.findByEvaluationId(evaluation.getId())));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!evaluationRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Evaluation not found"));
        }
        evaluationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping("/{evaluationId}/criteria")
    @Transactional
    public ResponseEntity<?> addCriterion(@PathVariable Long evaluationId, @Valid @RequestBody AddCriterionRequest request) {
        PlayerEvaluation evaluation = evaluationRepository.findById(evaluationId).orElse(null);
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
