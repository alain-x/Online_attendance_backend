package com.online.attendance.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateEmployeeRequest {

    @NotBlank
    private String employeeCode;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String department;

    private String mobile;

    private String designation;

    private String category;

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private String role;

    private BigDecimal hourlyRateOverride;
}
