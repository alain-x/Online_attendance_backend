package com.online.attendance.sports.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateChatRoomRequest {

    private Long teamId;

    @NotBlank
    private String name;

    @NotBlank
    private String type;

    private boolean isGroup;

    private List<Long> participantIds;
}
