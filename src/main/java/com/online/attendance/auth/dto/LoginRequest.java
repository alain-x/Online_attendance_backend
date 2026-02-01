package com.online.attendance.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    /** Optional. If blank, user is looked up by username across all companies. */
    private String companySlug;

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
