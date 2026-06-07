package com.online.attendance.sports.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChatRoomRequest {

    @NotNull
    private Long teamId;

    @NotBlank
    private String name;

    @NotBlank
    private String type;
}
