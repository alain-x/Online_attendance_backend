package com.online.attendance.leave.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecisionLeaveRequest {

    @NotNull
    private Boolean approve;

    private String note;
}
