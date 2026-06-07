package com.online.attendance.sports.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTrainingMaterialRequest {

    @NotNull
    private Long teamId;

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String fileUrl;

    @NotBlank
    private String fileType;
}
