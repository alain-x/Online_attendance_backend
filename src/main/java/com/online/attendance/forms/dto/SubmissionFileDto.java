package com.online.attendance.forms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionFileDto {
    private Long id;
    private Long submissionId;
    private String fieldKey;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
}
