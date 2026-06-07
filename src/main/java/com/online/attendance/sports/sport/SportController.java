package com.online.attendance.sports.sport;

import com.online.attendance.sports.sport.dto.SportResponse;
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
@RequestMapping("/api/sports/sports")
public class SportController {

    private static final Logger log = LoggerFactory.getLogger(SportController.class);
    private final SportRepository sportRepository;

    public SportController(SportRepository sportRepository) {
        this.sportRepository = sportRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<SportResponse> list() {
        return sportRepository.findByActiveTrue().stream()
                .map(SportResponse::from)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        var sport = sportRepository.findById(id);
        if (sport.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Sport not found"));
        }
        return ResponseEntity.ok(SportResponse.from(sport.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Sport sport) {
        if (sport.getName() == null || sport.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
        }
        Sport saved = sportRepository.save(sport);
        return ResponseEntity.ok(SportResponse.from(saved));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Sport updated) {
        var existing = sportRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Sport not found"));
        }
        var sport = existing.get();
        sport.setName(updated.getName());
        sport.setDescription(updated.getDescription());
        sport.setActive(updated.isActive());
        return ResponseEntity.ok(SportResponse.from(sportRepository.save(sport)));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!sportRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Sport not found"));
        }
        sportRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
