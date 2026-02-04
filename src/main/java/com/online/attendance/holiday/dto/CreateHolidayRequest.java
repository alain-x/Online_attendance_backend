package com.online.attendance.holiday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateHolidayRequest {

    @NotNull
    private LocalDate date;

    @NotBlank
    private String name;
}
