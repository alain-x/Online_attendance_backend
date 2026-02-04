package com.online.attendance.leave.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLeaveTypeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private boolean paid;

    private Boolean active;
}
