package com.online.attendance.forms.dto;

import com.online.attendance.forms.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpsertFormFieldRequest {

    @NotBlank
    @Size(max = 64)
    private String key;

    @NotBlank
    @Size(max = 200)
    private String label;

    @Size(max = 1000)
    private String description;

    @Size(max = 300)
    private String placeholder;

    @NotNull
    private FieldType type;

    @NotNull
    private Boolean required;

    @NotNull
    private Integer sortOrder;

    private String optionsJson;

    @Size(max = 300)
    private String accept;
}
