package com.online.attendance.security;

import com.online.attendance.company.Company;
import com.online.attendance.company.CompanyRepository;
import com.online.attendance.company.dto.CompanyResponse;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
import com.online.attendance.user.dto.UserCompanyContext;
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

    public Long requireCompanyId(Authentication authentication) {
        return requireCompanyId(authentication, null);
    }

    public Long requireCompanyId(Authentication authentication, Long overrideCompanyId) {
        return resolveCompanyId(authentication, overrideCompanyId);
    }

    public CompanyResponse requireCompanyResponse(Authentication authentication) {
        return requireCompanyResponse(authentication, null);
    }

    public CompanyResponse requireCompanyResponse(Authentication authentication, Long overrideCompanyId) {
        Long companyId = resolveCompanyId(authentication, overrideCompanyId);
        return companyRepository.findResponseById(companyId)
                .orElseThrow(() -> new IllegalStateException("Company not found"));
    }

    /** JPA reference only — avoids loading {@code logo_bytes} unless other fields are accessed. */
    public Company requireCompany(Authentication authentication) {
        return companyRepository.getReferenceById(requireCompanyId(authentication));
    }

    public Company requireCompany(Authentication authentication, Long overrideCompanyId) {
        return companyRepository.getReferenceById(requireCompanyId(authentication, overrideCompanyId));
    }

    private Long resolveCompanyId(Authentication authentication, Long overrideCompanyId) {
        UserCompanyContext ctx = userRepository.findUserCompanyContext(
                requireUsername(authentication),
                requireCompanySlug(authentication)
        ).orElseThrow(() -> new IllegalStateException("Company not set for user"));

        if (overrideCompanyId == null) {
            return ctx.companyId();
        }

        if (ctx.role() == Role.EMPLOYEE || ctx.role() == Role.RECORDER) {
            return ctx.companyId();
        }

        if (ctx.companyId().equals(overrideCompanyId)) {
            return overrideCompanyId;
        }

        if (ctx.role() == Role.SYSTEM_ADMIN) {
            return companyRepository.findResponseById(overrideCompanyId)
                    .map(CompanyResponse::id)
                    .orElseThrow(() -> new IllegalStateException("Company not found"));
        }

        CompanyResponse target = companyRepository.findResponseById(overrideCompanyId)
                .orElseThrow(() -> new IllegalStateException("Company not found"));
        if (target.parentCompanyId() != null && target.parentCompanyId().equals(ctx.companyId())) {
            return overrideCompanyId;
        }

        return ctx.companyId();
    }
}
