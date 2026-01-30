package com.online.attendance.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckOutRequest {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
