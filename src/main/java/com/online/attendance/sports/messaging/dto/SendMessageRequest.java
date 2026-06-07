package com.online.attendance.sports.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotBlank
    private String content;

    private String messageType;

    private String fileUrl;
}
