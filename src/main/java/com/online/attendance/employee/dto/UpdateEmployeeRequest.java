package com.online.attendance.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateEmployeeRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String department;

    private String mobile;

    private String designation;

    private String category;

    private String username;

    private String password;

    private String role;

    private Boolean enabled;

    private BigDecimal hourlyRateOverride;
}
