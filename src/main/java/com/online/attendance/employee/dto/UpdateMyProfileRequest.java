package com.online.attendance.employee.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMyProfileRequest {
    private String mobile;
    private String department;
    private String designation;
    private String category;
}
