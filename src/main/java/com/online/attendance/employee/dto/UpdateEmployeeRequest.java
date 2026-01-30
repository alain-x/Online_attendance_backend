package com.online.attendance.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

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

    private String password;

    private String role;

    private Boolean enabled;
}
