package com.online.attendance.forms.dto;

import com.online.attendance.forms.FileStorageMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpsertFormRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    private String companyLogoUrl;

    @NotNull
    private Boolean loginRequired;

    @NotNull
    private Boolean publicEnabled;

    @NotNull
    private Boolean active;

    @NotNull
    private FileStorageMode fileStorageMode;

    @Valid
    private List<UpsertFormFieldRequest> fields;
}
