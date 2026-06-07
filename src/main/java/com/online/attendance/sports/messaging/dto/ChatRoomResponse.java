package com.online.attendance.sports.messaging.dto;

import com.online.attendance.sports.messaging.ChatRoom;

import java.time.Instant;

public record ChatRoomResponse(
        Long id,
        Long teamId,
        String teamName,
        String name,
        String type,
        Long createdById,
        String createdByName,
        Instant createdAt
) {
    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getTeam() != null ? room.getTeam().getId() : null,
                room.getTeam() != null ? room.getTeam().getName() : null,
                room.getName(),
                room.getType(),
                room.getCreatedBy() != null ? room.getCreatedBy().getId() : null,
                room.getCreatedBy() != null ? room.getCreatedBy().getUsername() : null,
                room.getCreatedAt()
        );
    }
}
