package com.online.attendance.company.dto;

import com.online.attendance.company.Company;

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
        if (logoUrl != null && logoUrl.matches("/api/companies/\\d+/logo$")) {
            return logoUrl + "/image";
        }
        if (logoUrl != null && (logoUrl.startsWith("/uploads/") || logoUrl.startsWith("uploads/"))) {
            return companyId != null ? "/api/companies/" + companyId + "/logo/image" : null;
        }
        return logoUrl;
    }
}
