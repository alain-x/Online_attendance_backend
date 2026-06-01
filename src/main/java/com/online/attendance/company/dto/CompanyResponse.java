package com.online.attendance.company.dto;

import com.online.attendance.company.Company;
import com.online.attendance.company.CompanyLogoUrls;

import java.math.BigDecimal;

public record CompanyResponse(
        Long id,
        String name,
        String slug,
        String logoUrl,
        BigDecimal hourlyRateDefault,
        boolean active,
        Long parentCompanyId
) {
    public CompanyResponse {
        logoUrl = normalizeLogoUrl(logoUrl, id);
    }

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getLogoUrl(),
                company.getHourlyRateDefault(),
                company.isActive(),
                company.getParentCompanyId()
        );
    }

    private static String normalizeLogoUrl(String logoUrl, Long companyId) {
        return CompanyLogoUrls.normalizeResponseUrl(logoUrl, companyId);
    }
}
