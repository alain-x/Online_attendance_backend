package com.online.attendance.security;

import com.online.attendance.company.Company;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentCompanyService {

    private final UserRepository userRepository;

    public CurrentCompanyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String requireUsername(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        String principal = authentication.getName();
        int idx = principal != null ? principal.indexOf("::") : -1;
        if (idx <= 0) {
            throw new IllegalStateException("Invalid principal format");
        }
        return principal.substring(idx + 2);
    }

    public String requireCompanySlug(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        String principal = authentication.getName();
        int idx = principal != null ? principal.indexOf("::") : -1;
        if (idx <= 0) {
            throw new IllegalStateException("Invalid principal format");
        }
        return principal.substring(0, idx);
    }

    public Company requireCompany(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Unauthenticated");
        }

        String companySlug = requireCompanySlug(authentication);
        String username = requireUsername(authentication);

        AppUser user = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (user == null || user.getCompany() == null) {
            throw new IllegalStateException("Company not set for user");
        }

        return user.getCompany();
    }
}
