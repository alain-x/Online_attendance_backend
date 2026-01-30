package com.online.attendance.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    private String companySlug;

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
