package com.online.attendance.user.dto;

import com.online.attendance.user.Role;

public record UserCompanyContext(Role role, Long companyId, Long parentCompanyId) {}
