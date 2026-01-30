package com.online.attendance.location.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkLocationRequest {

    @NotBlank
    private String name;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @NotNull
    @Min(1)
    private Integer radiusMeters;

    @NotNull
    private Boolean active;
}
