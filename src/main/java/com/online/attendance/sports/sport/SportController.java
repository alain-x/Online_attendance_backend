package com.online.attendance.sports.sport;

import com.online.attendance.sports.sport.dto.CreateSportRequest;
import com.online.attendance.sports.sport.dto.SportResponse;
import com.online.attendance.sports.sport.dto.UpdateSportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/sports")
public class SportController {

    private static final Logger log = LoggerFactory.getLogger(SportController.class);
    private final SportRepository sportRepository;

    public SportController(SportRepository sportRepository) {
        this.sportRepository = sportRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<SportResponse> list() {
        return sportRepository.findByActiveTrue().stream()
                .map(SportResponse::from)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        var sport = sportRepository.findById(id);
        if (sport.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Sport not found"));
        }
        return ResponseEntity.ok(SportResponse.from(sport.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateSportRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
        }
        Sport sport = Sport.builder()
                .name(req.getName().trim())
                .description(req.getDescription())
                .build();
        Sport saved = sportRepository.save(sport);
        return ResponseEntity.ok(SportResponse.from(saved));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateSportRequest req) {
        var existing = sportRepository.findById(id);
        if (existing.isEmpty()) {
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
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!sportRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Sport not found"));
        }
        sportRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
