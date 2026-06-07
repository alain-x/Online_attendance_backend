package com.online.attendance.sports.messaging.dto;

import com.online.attendance.sports.messaging.ChatMessage;

import java.time.Instant;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        Long senderId,
        String senderName,
        String content,
        String messageType,
        String fileUrl,
        Instant createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom() != null ? message.getRoom().getId() : null,
                message.getSender() != null ? message.getSender().getId() : null,
                message.getSender() != null ? message.getSender().getUsername() : null,
                message.getContent(),
                message.getMessageType(),
                message.getFileUrl(),
                message.getCreatedAt()
        );
    }
}
