package com.online.attendance.sports.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateCalendarEventRequest {

    @NotNull
    private Long teamId;

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String eventType;

    @NotNull
    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private String location;

    private boolean allDay;

    private String color;
}
