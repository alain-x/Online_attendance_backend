package com.online.attendance.sports.payment;

import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.sports.club.SportsClub;
import com.online.attendance.sports.club.SportsClubRepository;
import com.online.attendance.sports.payment.dto.*;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/payments")
@Transactional(readOnly = true)
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final MembershipFeeRepository feeRepository;
    private final PlayerPaymentRepository paymentRepository;
    private final SportsClubRepository clubRepository;
    private final TeamRepository teamRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final CurrentCompanyService currentCompanyService;

    public PaymentController(MembershipFeeRepository feeRepository, PlayerPaymentRepository paymentRepository,
                             SportsClubRepository clubRepository, TeamRepository teamRepository,
                             PlayerProfileRepository playerProfileRepository,
                             CurrentCompanyService currentCompanyService) {
        this.feeRepository = feeRepository;
        this.paymentRepository = paymentRepository;
        this.clubRepository = clubRepository;
        this.teamRepository = teamRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/fees")
    public List<FeeResponse> listFees(@RequestParam(required = false) Long clubId,
                                       @RequestParam(required = false) Long teamId,
                                       Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        List<MembershipFee> fees;
        if (clubId != null) {
            var club = clubRepository.findByIdAndCompanyId(clubId, companyId);
            if (club.isEmpty()) {
                return List.of();
            }
            fees = feeRepository.findByClubId(clubId);
        } else if (teamId != null) {
            var team = teamRepository.findByIdAndClubCompanyId(teamId, companyId);
            if (team.isEmpty()) {
                return List.of();
            }
            fees = feeRepository.findByTeamId(teamId);
        } else {
            fees = feeRepository.findByClubCompanyId(companyId);
        }
        return fees.stream().map(FeeResponse::from).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/fees/{id}")
    public ResponseEntity<?> getFeeById(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var fee = feeRepository.findByIdAndClubCompanyId(id, companyId);
        if (fee.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Fee not found"));
        }
        return ResponseEntity.ok(FeeResponse.from(fee.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PostMapping("/fees")
    @Transactional
    public ResponseEntity<?> createFee(@Valid @RequestBody CreateFeeRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        SportsClub club = clubRepository.findByIdAndCompanyId(request.getClubId(), companyId).orElse(null);
        if (club == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Club not found"));
        }
        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        }
        MembershipFee fee = MembershipFee.builder()
                .club(club)
                .team(team)
                .name(request.getName())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .frequency(request.getFrequency())
                .description(request.getDescription())
                .build();
        fee = feeRepository.save(fee);
        return ResponseEntity.ok(FeeResponse.from(fee));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PutMapping("/fees/{id}")
    @Transactional
    public ResponseEntity<?> updateFee(@PathVariable Long id, @Valid @RequestBody CreateFeeRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var existing = feeRepository.findByIdAndClubCompanyId(id, companyId);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Fee not found"));
        }
        var fee = existing.get();
        SportsClub club = clubRepository.findByIdAndCompanyId(request.getClubId(), companyId).orElse(null);
        if (club == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Club not found"));
        }
        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        }
        fee.setClub(club);
        fee.setTeam(team);
        fee.setName(request.getName());
        fee.setAmount(request.getAmount());
        fee.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        fee.setFrequency(request.getFrequency());
        fee.setDescription(request.getDescription());
        fee = feeRepository.save(fee);
        return ResponseEntity.ok().body(FeeResponse.from(fee));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @DeleteMapping("/fees/{id}")
    @Transactional
    public ResponseEntity<?> deleteFee(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var fee = feeRepository.findByIdAndClubCompanyId(id, companyId);
        if (fee.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Fee not found"));
        }
        feeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<PaymentResponse> listPayments(@RequestParam(required = false) Long playerId,
                                               @RequestParam(required = false) Long feeId,
                                               @RequestParam(required = false) String status,
                                               Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        List<PlayerPayment> payments;
        if (playerId != null) {
            var player = playerProfileRepository.findByIdAndClubCompanyId(playerId, companyId);
            if (player.isEmpty()) {
                return List.of();
            }
            payments = paymentRepository.findByPlayerId(playerId);
        } else if (feeId != null) {
            var fee = feeRepository.findByIdAndClubCompanyId(feeId, companyId);
            if (fee.isEmpty()) {
                return List.of();
            }
            payments = paymentRepository.findByFeeId(feeId);
        } else {
            payments = paymentRepository.findByClubCompanyId(companyId);
            if (status != null) {
                payments = payments.stream()
                        .filter(p -> status.equals(p.getStatus()))
                        .collect(Collectors.toList());
            }
        }
        return payments.stream().map(PaymentResponse::from).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> recordPayment(@Valid @RequestBody RecordPaymentRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        MembershipFee fee = feeRepository.findByIdAndClubCompanyId(request.getFeeId(), companyId).orElse(null);
        if (fee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Fee not found"));
        }
        PlayerProfile player = playerProfileRepository.findByIdAndClubCompanyId(request.getPlayerId(), companyId).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Player not found"));
        }
        PlayerPayment payment = PlayerPayment.builder()
                .fee(fee)
                .player(player)
                .amount(request.getAmount())
                .currency(fee.getCurrency())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .createdAt(Instant.now())
                .build();
        payment = paymentRepository.save(payment);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updatePaymentStatus(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var existing = paymentRepository.findByIdAndClubCompanyId(id, companyId);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Payment not found"));
        }
        var payment = existing.get();
        String status = body.get("status");
        payment.setStatus(status);
        if ("PAID".equalsIgnoreCase(status)) {
            payment.setPaidDate(LocalDate.now());
        }
        payment = paymentRepository.save(payment);
        return ResponseEntity.ok().body(PaymentResponse.from(payment));
    }
}
