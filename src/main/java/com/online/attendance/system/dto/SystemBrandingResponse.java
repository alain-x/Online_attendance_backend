package com.online.attendance.system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SystemBrandingResponse {
    private final String logoUrl;
    private final String faviconUrl;
    private final String systemName;
}
