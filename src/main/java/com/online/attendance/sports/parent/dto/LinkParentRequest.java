package com.online.attendance.sports.parent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkParentRequest {

    @NotNull
    private Long parentUserId;

    @NotNull
    private Long playerId;

    private String relationship;
}
