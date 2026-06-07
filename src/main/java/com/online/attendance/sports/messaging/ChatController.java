package com.online.attendance.sports.messaging;

import com.online.attendance.sports.messaging.dto.*;
import com.online.attendance.sports.team.Team;
import com.online.attendance.sports.team.TeamRepository;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
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

    @Value("${app.upload.dir:uploads/chat}")
    private String uploadDir;

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
    public ResponseEntity<?> listRooms(Authentication authentication, @RequestParam(required = false) Long teamId) {
        AppUser user = resolveUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }
        List<ChatRoom> rooms;
        if (teamId != null) {
            rooms = roomRepository.findByTeamId(teamId);
        } else {
            rooms = roomRepository.findMyRooms(user.getId());
        }
        return ResponseEntity.ok(rooms.stream().map(this::toRoomResponse).collect(Collectors.toList()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/rooms/all")
    public List<ChatRoomResponse> listAllRooms() {
        return roomRepository.findAll().stream().map(this::toRoomResponse).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(Authentication authentication, @Valid @RequestBody CreateChatRoomRequest request) {
        AppUser creator = resolveUser(authentication);
        if (creator == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }
        Team team = request.getTeamId() != null ? teamRepository.findById(request.getTeamId()).orElse(null) : null;

        ChatRoom newRoom = ChatRoom.builder()
                .team(team)
                .name(request.getName())
                .type(request.getType())
                .isGroup(request.isGroup())
                .createdBy(creator)
                .createdAt(Instant.now())
                .build();
        ChatRoom savedRoom = roomRepository.save(newRoom);

        participantRepository.save(ChatParticipant.builder()
                .room(savedRoom)
                .user(creator)
                .joinedAt(Instant.now())
                .build());

        if (request.getParticipantIds() != null) {
            for (Long pid : request.getParticipantIds()) {
                if (pid.equals(creator.getId())) continue;
                userRepository.findById(pid).ifPresent(u -> {
                    boolean already = participantRepository.findByRoomId(savedRoom.getId()).stream()
                            .anyMatch(p -> p.getUser().getId().equals(pid));
                    if (!already) {
                        participantRepository.save(ChatParticipant.builder()
                                .room(savedRoom).user(u).joinedAt(Instant.now()).build());
                    }
                });
            }
        }

        return ResponseEntity.ok(toRoomResponse(savedRoom));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @PostMapping("/direct")
    public ResponseEntity<?> createOrGetDirectChat(Authentication authentication, @RequestBody Map<String, Long> body) {
        Long targetUserId = body.get("userId");
        if (targetUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId is required"));
        }
        AppUser current = resolveUser(authentication);
        if (current == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }
        if (current.getId().equals(targetUserId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot chat with yourself"));
        }
        AppUser target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        Optional<ChatRoom> existing = roomRepository.findDirectChat(current.getId(), targetUserId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(toRoomResponse(existing.get()));
        }

        ChatRoom newRoom = ChatRoom.builder()
                .name(target.getUsername())
                .type("DIRECT")
                .isGroup(false)
                .createdBy(current)
                .createdAt(Instant.now())
                .build();
        newRoom = roomRepository.save(newRoom);
        ChatRoom savedRoom = newRoom;

        participantRepository.save(ChatParticipant.builder().room(savedRoom).user(current).joinedAt(Instant.now()).build());
        participantRepository.save(ChatParticipant.builder().room(savedRoom).user(target).joinedAt(Instant.now()).build());

        return ResponseEntity.ok(toRoomResponse(savedRoom));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/rooms/{roomId}/participants")
    public List<Map<String, Object>> listParticipants(@PathVariable Long roomId) {
        return participantRepository.findByRoomId(roomId).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("userId", p.getUser().getId());
            m.put("username", p.getUser().getUsername());
            m.put("joinedAt", p.getJoinedAt());
            return m;
        }).collect(Collectors.toList());
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
    @DeleteMapping("/rooms/{roomId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(@PathVariable Long roomId, @PathVariable Long userId) {
        List<ChatParticipant> participants = participantRepository.findByRoomId(roomId);
        participants.stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst().ifPresent(participantRepository::delete);
        return ResponseEntity.noContent().build();
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
        if (sender == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }
        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent() != null ? request.getContent() : "")
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .createdAt(Instant.now())
                .build();
        message = messageRepository.save(message);
        return ResponseEntity.ok(ChatMessageResponse.from(message));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty"));
        }
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID().toString() + ext;
            Path targetPath = uploadPath.resolve(storedName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            String fileUrl = "/api/sports/chat/files/" + storedName;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("fileName", originalName);
            result.put("fileSize", file.getSize());
            result.put("mimeType", file.getContentType());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("File upload failed", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "File upload failed"));
        }
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/files/{filename}")
    public ResponseEntity<?> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";
            byte[] data = Files.readAllBytes(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(Authentication authentication, @RequestParam("q") String query) {
        AppUser current = resolveUser(authentication);
        if (current == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }
        String q = query.trim().toLowerCase();
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(current.getId()))
                .filter(u -> u.getUsername().toLowerCase().contains(q)
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("email", u.getEmail());
                    m.put("role", u.getRole().name());
                    return m;
                })
                .limit(20)
                .collect(Collectors.toList()));
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room) {
        List<ChatParticipant> participants = participantRepository.findByRoomId(room.getId());
        int participantCount = participants.size();
        ChatMessageResponse lastMsg = messageRepository.findTopByRoomIdOrderByCreatedAtDesc(room.getId())
                .map(ChatMessageResponse::from).orElse(null);
        return new ChatRoomResponse(
                room.getId(),
                room.getTeam() != null ? room.getTeam().getId() : null,
                room.getTeam() != null ? room.getTeam().getName() : null,
                room.getName(),
                room.getType(),
                room.isGroup(),
                room.getCreatedBy() != null ? room.getCreatedBy().getId() : null,
                room.getCreatedBy() != null ? room.getCreatedBy().getUsername() : null,
                room.getCreatedAt(),
                participantCount,
                lastMsg != null ? lastMsg.id() : null,
                lastMsg != null ? lastMsg.content() : null,
                lastMsg != null ? lastMsg.senderName() : null,
                lastMsg != null ? lastMsg.createdAt() : null
        );
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
}
