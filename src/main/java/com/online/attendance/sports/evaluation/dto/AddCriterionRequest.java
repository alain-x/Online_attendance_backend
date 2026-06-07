package com.online.attendance.sports.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCriterionRequest {

    @NotBlank
    private String criterionName;

    @Min(1)
    @Max(10)
    private Integer score;

    private String notes;
}
