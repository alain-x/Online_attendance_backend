package com.online.attendance.sports.sport;

import com.online.attendance.company.CompanyRepository;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.sports.sport.dto.CreateSportRequest;
import com.online.attendance.sports.sport.dto.SportResponse;
import com.online.attendance.sports.sport.dto.UpdateSportRequest;
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
@RequestMapping("/api/sports/sports")
@Transactional(readOnly = true)
public class SportController {

    private static final Logger log = LoggerFactory.getLogger(SportController.class);
    private final SportRepository sportRepository;
    private final CompanyRepository companyRepository;
    private final CurrentCompanyService currentCompanyService;

    public SportController(SportRepository sportRepository,
                           CompanyRepository companyRepository,
                           CurrentCompanyService currentCompanyService) {
        this.sportRepository = sportRepository;
        this.companyRepository = companyRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<SportResponse> list(Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        return sportRepository.findByCompanyIdAndActiveTrue(companyId).stream()
                .map(SportResponse::from)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var sport = sportRepository.findById(id);
        if (sport.isEmpty() || !sport.get().getCompany().getId().equals(companyId)) {
            return ResponseEntity.status(404).body(Map.of("message", "Sport not found"));
        }
        return ResponseEntity.ok(SportResponse.from(sport.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody CreateSportRequest req, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        if (req.getName() == null || req.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
        }
        Sport sport = Sport.builder()
                .company(companyRepository.getReferenceById(companyId))
                .name(req.getName().trim())
                .description(req.getDescription())
                .build();
        Sport saved = sportRepository.save(sport);
        return ResponseEntity.ok(SportResponse.from(saved));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateSportRequest req, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var existing = sportRepository.findById(id);
        if (existing.isEmpty() || !existing.get().getCompany().getId().equals(companyId)) {
            return ResponseEntity.status(404).body(Map.of("message", "Sport not found"));
        }
        var sport = existing.get();
        sport.setName(req.getName().trim());
        sport.setDescription(req.getDescription());
        sport.setActive(req.isActive());
        return ResponseEntity.ok(SportResponse.from(sportRepository.save(sport)));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var sport = sportRepository.findById(id);
        if (sport.isEmpty() || !sport.get().getCompany().getId().equals(companyId)) {
            return ResponseEntity.status(404).body(Map.of("message", "Sport not found"));
        }
        sportRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
