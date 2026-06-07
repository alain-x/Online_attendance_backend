package com.online.attendance.sports.messaging;

import com.online.attendance.sports.messaging.dto.*;
import com.online.attendance.sports.team.Team;
import com.online.attendance.sports.team.TeamRepository;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public ChatController(ChatRoomRepository roomRepository, ChatMessageRepository messageRepository,
                          ChatParticipantRepository participantRepository, TeamRepository teamRepository,
                          UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/rooms")
    public List<ChatRoomResponse> listRooms(@RequestParam(required = false) Long teamId) {
        List<ChatRoom> rooms;
        if (teamId != null) {
            rooms = roomRepository.findByTeamId(teamId);
        } else {
            rooms = roomRepository.findAll();
        }
        return rooms.stream().map(ChatRoomResponse::from).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(Authentication authentication, @Valid @RequestBody CreateChatRoomRequest request) {
        Team team = teamRepository.findById(request.getTeamId()).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        AppUser creator = resolveUser(authentication);
        ChatRoom room = ChatRoom.builder()
                .team(team)
                .name(request.getName())
                .type(request.getType())
                .createdBy(creator)
                .createdAt(Instant.now())
                .build();
        room = roomRepository.save(room);
        return ResponseEntity.ok(ChatRoomResponse.from(room));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @PostMapping("/rooms/{roomId}/participants")
    public ResponseEntity<?> addParticipant(@PathVariable Long roomId, @RequestBody Map<String, Long> body) {
        ChatRoom room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Room not found"));
        }
        Long userId = body.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId is required"));
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }
        boolean alreadyParticipant = participantRepository.findByRoomId(roomId).stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
        if (alreadyParticipant) {
            return ResponseEntity.status(409).body(Map.of("message", "User is already a participant"));
        }
        ChatParticipant participant = ChatParticipant.builder()
                .room(room)
                .user(user)
                .joinedAt(Instant.now())
                .build();
        participant = participantRepository.save(participant);
        return ResponseEntity.ok(Map.of("id", participant.getId(), "userId", userId, "roomId", roomId));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatMessageResponse> listMessages(@PathVariable Long roomId) {
        return messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> sendMessage(Authentication authentication, @PathVariable Long roomId, @Valid @RequestBody SendMessageRequest request) {
        ChatRoom room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Room not found"));
        }
        AppUser sender = resolveUser(authentication);
        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .fileUrl(request.getFileUrl())
                .createdAt(Instant.now())
                .build();
        message = messageRepository.save(message);
        return ResponseEntity.ok(ChatMessageResponse.from(message));
    }

    private AppUser resolveUser(Authentication authentication) {
        String principal = authentication.getName();
        int idx = principal != null ? principal.indexOf("::") : -1;
        String username = (idx > 0 && idx + 2 < principal.length()) ? principal.substring(idx + 2) : principal;
        return userRepository.findByUsername(username).orElse(null);
    }
}
