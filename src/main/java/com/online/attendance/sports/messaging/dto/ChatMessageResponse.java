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
        String fileName,
        Long fileSize,
        String mimeType,
        Instant createdAt,
        Long parentMessageId,
        String parentContent,
        String parentSenderName
) {
    public static ChatMessageResponse from(ChatMessage message) {
        ChatMessage parent = message.getParentMessage();
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom() != null ? message.getRoom().getId() : null,
                message.getSender() != null ? message.getSender().getId() : null,
                message.getSender() != null ? message.getSender().getUsername() : null,
                message.getContent(),
                message.getMessageType(),
                message.getFileUrl(),
                message.getFileName(),
                message.getFileSize(),
                message.getMimeType(),
                message.getCreatedAt(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getContent() : null,
                parent != null && parent.getSender() != null ? parent.getSender().getUsername() : null
        );
    }
}
