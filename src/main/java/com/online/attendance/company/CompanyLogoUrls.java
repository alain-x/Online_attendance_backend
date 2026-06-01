package com.online.attendance.company;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompanyLogoUrls {

    private static final Pattern API_LOGO_PATH = Pattern.compile("/api/companies/(\\d+)/logo(?:/image)?");

    private CompanyLogoUrls() {
    }

    public static String apiImagePath(Long companyId) {
        return companyId != null ? "/api/companies/" + companyId + "/logo/image" : null;
    }

    /** Canonical relative path stored in the database. */
    public static String normalizeStoredUrl(String logoUrl, Long companyId) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return null;
        }
        String trimmed = logoUrl.trim();
        Matcher matcher = API_LOGO_PATH.matcher(trimmed);
        if (matcher.find()) {
            long id = companyId != null ? companyId : Long.parseLong(matcher.group(1));
            return apiImagePath(id);
        }
        if (trimmed.startsWith("/uploads/") || trimmed.startsWith("uploads/")) {
            return apiImagePath(companyId);
        }
        return trimmed;
    }

    /** Value returned from list/get APIs (relative API path when possible). */
    public static String normalizeResponseUrl(String logoUrl, Long companyId) {
        return normalizeStoredUrl(logoUrl, companyId);
    }

    public static boolean isApiLogoPath(String logoUrl) {
        return logoUrl != null && API_LOGO_PATH.matcher(logoUrl).find();
    }
}
