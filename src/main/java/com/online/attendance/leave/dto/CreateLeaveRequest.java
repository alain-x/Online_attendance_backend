package com.online.attendance.leave.dto;

import com.online.attendance.leave.HalfDayPart;
import com.online.attendance.leave.LeaveUnit;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CreateLeaveRequest {

    private Long employeeId;

    @NotNull
    private Long leaveTypeId;

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    @NotNull
    private LeaveUnit unit;

    private HalfDayPart halfDayPart;

    private LocalTime startTime;

    private LocalTime endTime;

    private String reason;
}
