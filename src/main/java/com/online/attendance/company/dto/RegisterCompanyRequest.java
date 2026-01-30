package com.online.attendance.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterCompanyRequest {

    @NotBlank
    @Size(max = 200)
    private String companyName;

    @NotBlank
    @Size(max = 100)
    private String companySlug;

    @NotBlank
    @Size(max = 100)
    @Email
    private String adminUsername;

    @NotBlank
    private String adminPassword;
}
