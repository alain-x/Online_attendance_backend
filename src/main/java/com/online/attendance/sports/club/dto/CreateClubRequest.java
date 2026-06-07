package com.online.attendance.sports.club.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClubRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    private String contactEmail;

    private String contactPhone;

    private String address;
}
