package com.online.attendance.sports.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdAndDeletedFalseOrderByCreatedAtAsc(Long roomId);

    @Query("SELECT m FROM ChatMessage m WHERE m.room.id = :roomId AND m.deleted = false AND m.id NOT IN (SELECT h.messageId FROM MessageHiddenBy h WHERE h.userId = :userId) ORDER BY m.createdAt ASC")
    List<ChatMessage> findByRoomIdAndNotHidden(@Param("roomId") Long roomId, @Param("userId") Long userId);

    Optional<ChatMessage> findTopByRoomIdAndDeletedFalseOrderByCreatedAtDesc(Long roomId);
    Optional<ChatMessage> findByIdAndRoomId(Long id, Long roomId);
}
