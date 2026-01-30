package com.online.attendance.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String role;
    private boolean enabled;
    private Long companyId;
    private String companySlug;
}
