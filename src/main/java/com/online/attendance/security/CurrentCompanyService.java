package com.online.attendance.security;

import com.online.attendance.company.Company;
import com.online.attendance.company.CompanyRepository;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentCompanyService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public CurrentCompanyService(UserRepository userRepository, CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
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

    public Company requireCompany(Authentication authentication, Long overrideCompanyId) {
        if (overrideCompanyId == null) {
            return requireCompany(authentication);
        }

        String companySlug = requireCompanySlug(authentication);
        String username = requireUsername(authentication);

        AppUser user = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (user == null || user.getCompany() == null || user.getCompany().getId() == null) {
            throw new IllegalStateException("Company not set for user");
        }

        // Employees must never be able to switch context
        if (user.getRole() == Role.EMPLOYEE || user.getRole() == Role.RECORDER) {
            return user.getCompany();
        }

        Long currentCompanyId = user.getCompany().getId();
        if (currentCompanyId.equals(overrideCompanyId)) {
            return user.getCompany();
        }

        if (user.getRole() == Role.SYSTEM_ADMIN) {
            return companyRepository.findById(overrideCompanyId)
                    .orElseThrow(() -> new IllegalStateException("Company not found"));
        }

        // Owner-admin use case: parent company can view/manage its direct branches.
        Company target = companyRepository.findById(overrideCompanyId)
                .orElseThrow(() -> new IllegalStateException("Company not found"));
        Long parentId = target.getParentCompanyId();
        if (parentId != null && parentId.equals(currentCompanyId)) {
            return target;
        }

        // otherwise, keep tenant boundary
        return user.getCompany();
    }
}
