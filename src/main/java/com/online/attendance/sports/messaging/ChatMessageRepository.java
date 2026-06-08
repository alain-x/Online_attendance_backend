package com.online.attendance.sports.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdAndDeletedFalseOrderByCreatedAtAsc(Long roomId);
    Optional<ChatMessage> findTopByRoomIdAndDeletedFalseOrderByCreatedAtDesc(Long roomId);
    Optional<ChatMessage> findByIdAndRoomId(Long id, Long roomId);
}
