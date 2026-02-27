package com.online.attendance.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    private String email;

    private String password;

    private String role;

    private Boolean enabled;
}
