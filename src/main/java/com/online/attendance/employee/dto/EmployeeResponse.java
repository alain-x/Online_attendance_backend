package com.online.attendance.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String department;
    private String mobile;
    private String designation;
    private String category;
    private String username;
    private String email;
    private String role;

    private BigDecimal hourlyRateOverride;
}
