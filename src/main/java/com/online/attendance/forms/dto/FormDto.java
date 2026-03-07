package com.online.attendance.forms.dto;

import com.online.attendance.forms.FileStorageMode;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormDto {
    private Long id;
    private Long companyId;
    private String title;
    private String description;
    private String companyLogoUrl;
    private boolean loginRequired;
    private boolean publicEnabled;
    private String publicToken;
    private FileStorageMode fileStorageMode;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private List<FormFieldDto> fields;
}
