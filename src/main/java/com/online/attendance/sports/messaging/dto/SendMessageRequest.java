package com.online.attendance.sports.messaging.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    private String content;

    private String messageType;

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private String mimeType;
}
