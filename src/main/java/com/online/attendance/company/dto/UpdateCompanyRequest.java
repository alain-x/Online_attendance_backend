package com.online.attendance.company.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCompanyRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 100)
    private String slug;

    @Size(max = 500)
    private String logoUrl;
}
