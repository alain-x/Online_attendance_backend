package com.online.attendance.sports.club;

import com.online.attendance.company.CompanyRepository;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.sports.club.dto.ClubResponse;
import com.online.attendance.sports.club.dto.CreateClubRequest;
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
@RequestMapping("/api/sports/clubs")
@Transactional(readOnly = true)
public class ClubController {

    private static final Logger log = LoggerFactory.getLogger(ClubController.class);
    private final SportsClubRepository clubRepository;
    private final CompanyRepository companyRepository;
    private final CurrentCompanyService currentCompanyService;

    public ClubController(SportsClubRepository clubRepository,
                          CompanyRepository companyRepository,
                          CurrentCompanyService currentCompanyService) {
        this.clubRepository = clubRepository;
        this.companyRepository = companyRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<ClubResponse> list(Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        return clubRepository.findByCompanyId(companyId).stream()
                .map(ClubResponse::from)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var club = clubRepository.findByIdAndCompanyId(id, companyId);
        if (club.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Club not found"));
        }
        return ResponseEntity.ok(ClubResponse.from(club.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreateClubRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        if (clubRepository.existsBySlugAndCompanyId(request.getSlug(), companyId)) {
            return ResponseEntity.status(409).body(Map.of("message", "Slug already exists"));
        }
        SportsClub club = SportsClub.builder()
                .company(companyRepository.getReferenceById(companyId))
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .build();
        club = clubRepository.save(club);
        return ResponseEntity.ok(ClubResponse.from(club));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateClubRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var existing = clubRepository.findByIdAndCompanyId(id, companyId);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Club not found"));
        }
        var club = existing.get();
        if (!club.getSlug().equals(request.getSlug()) && clubRepository.existsBySlugAndCompanyId(request.getSlug(), companyId)) {
            return ResponseEntity.status(409).body(Map.of("message", "Slug already exists"));
        }
        club.setName(request.getName());
        club.setSlug(request.getSlug());
        club.setDescription(request.getDescription());
        club.setContactEmail(request.getContactEmail());
        club.setContactPhone(request.getContactPhone());
        club.setAddress(request.getAddress());
        return ResponseEntity.ok().body(ClubResponse.from(clubRepository.save(club)));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var club = clubRepository.findByIdAndCompanyId(id, companyId);
        if (club.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Club not found"));
        }
        clubRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @PostMapping("/backfill-company")
    @Transactional
    public ResponseEntity<?> backfillCompany(Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        long orphaned = clubRepository.countOrphaned();
        if (orphaned == 0) {
            return ResponseEntity.ok(Map.of("message", "No orphaned clubs found", "updated", 0));
        }
        int updated = clubRepository.backfillCompanyId(companyId);
        log.info("Backfilled {} clubs with companyId={}", updated, companyId);
        return ResponseEntity.ok(Map.of("message", "Backfill complete", "updated", updated));
    }
}
