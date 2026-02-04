package com.online.attendance.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSystemBrandingRequest {

    @Size(max = 200)
    private String systemName;
}
