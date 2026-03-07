package com.online.attendance.forms.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionDto {
    private Long id;
    private Long formId;
    private Long companyId;
    private Instant submittedAt;
    private String submittedByUsername;
    private Long submittedByUserId;
    private String answersJson;
}
