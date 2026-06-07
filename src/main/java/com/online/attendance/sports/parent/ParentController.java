package com.online.attendance.sports.parent;

import com.online.attendance.sports.parent.dto.LinkParentRequest;
import com.online.attendance.sports.parent.dto.ParentLinkResponse;
import com.online.attendance.sports.player.PlayerProfile;
import com.online.attendance.sports.player.PlayerProfileRepository;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/parents")
public class ParentController {

    private static final Logger log = LoggerFactory.getLogger(ParentController.class);
    private final ParentLinkRepository parentLinkRepository;
    private final UserRepository userRepository;
    private final PlayerProfileRepository playerProfileRepository;

    public ParentController(ParentLinkRepository parentLinkRepository, UserRepository userRepository,
                            PlayerProfileRepository playerProfileRepository) {
        this.parentLinkRepository = parentLinkRepository;
        this.userRepository = userRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @PostMapping
    public ResponseEntity<?> linkParent(@Valid @RequestBody LinkParentRequest request) {
        AppUser parentUser = userRepository.findById(request.getParentUserId()).orElse(null);
        if (parentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Parent user not found"));
        }
        PlayerProfile player = playerProfileRepository.findById(request.getPlayerId()).orElse(null);
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Player not found"));
        }
        boolean alreadyLinked = parentLinkRepository.findByParentUserId(request.getParentUserId()).stream()
                .anyMatch(l -> l.getPlayer().getId().equals(request.getPlayerId()));
        if (alreadyLinked) {
            return ResponseEntity.status(409).body(Map.of("message", "Parent already linked to this player"));
        }
        ParentLink link = ParentLink.builder()
                .parentUser(parentUser)
                .player(player)
                .relationship(request.getRelationship())
                .createdAt(Instant.now())
                .build();
        link = parentLinkRepository.save(link);
        return ResponseEntity.ok(ParentLinkResponse.from(link));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> unlink(@PathVariable Long id) {
        ParentLink link = parentLinkRepository.findById(id).orElse(null);
        if (link == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Parent link not found"));
        }
        parentLinkRepository.delete(link);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'PARENT')")
    @GetMapping("/children")
    public List<ParentLinkResponse> getMyChildren(@RequestParam Long parentUserId) {
        return parentLinkRepository.findByParentUserId(parentUserId).stream()
                .map(ParentLinkResponse::from)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PARENT')")
    @GetMapping("/player/{playerId}")
    public List<ParentLinkResponse> getParents(@PathVariable Long playerId) {
        return parentLinkRepository.findByPlayerId(playerId).stream()
                .map(ParentLinkResponse::from)
                .collect(Collectors.toList());
    }
}
