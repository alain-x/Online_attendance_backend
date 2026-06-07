package com.online.attendance.sports.parent.dto;

import com.online.attendance.sports.parent.ParentLink;

import java.time.Instant;

public record ParentLinkResponse(
        Long id,
        Long parentUserId,
        String parentName,
        Long playerId,
        String playerName,
        String relationship,
        Instant createdAt
) {
    public static ParentLinkResponse from(ParentLink link) {
        return new ParentLinkResponse(
                link.getId(),
                link.getParentUser() != null ? link.getParentUser().getId() : null,
                link.getParentUser() != null ? link.getParentUser().getUsername() : null,
                link.getPlayer() != null ? link.getPlayer().getId() : null,
                link.getPlayer() != null && link.getPlayer().getUser() != null ? link.getPlayer().getUser().getUsername() : null,
                link.getRelationship(),
                link.getCreatedAt()
        );
    }
}
