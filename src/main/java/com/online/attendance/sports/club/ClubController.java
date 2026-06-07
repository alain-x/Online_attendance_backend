package com.online.attendance.sports.club;

import com.online.attendance.sports.club.dto.ClubResponse;
import com.online.attendance.sports.club.dto.CreateClubRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/clubs")
public class ClubController {

    private static final Logger log = LoggerFactory.getLogger(ClubController.class);
    private final SportsClubRepository clubRepository;

    public ClubController(SportsClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<ClubResponse> list() {
        return clubRepository.findAll().stream()
                .map(ClubResponse::from)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        var club = clubRepository.findById(id);
        if (club.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Club not found"));
        }
        return ResponseEntity.ok(ClubResponse.from(club.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateClubRequest request) {
        if (clubRepository.existsBySlug(request.getSlug())) {
            return ResponseEntity.status(409).body(Map.of("message", "Slug already exists"));
        }
        SportsClub club = SportsClub.builder()
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

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateClubRequest request) {
        var existing = clubRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Club not found"));
        }
        var club = existing.get();
        if (!club.getSlug().equals(request.getSlug()) && clubRepository.existsBySlug(request.getSlug())) {
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

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!clubRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Club not found"));
        }
        clubRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
