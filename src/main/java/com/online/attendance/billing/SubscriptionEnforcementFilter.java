package com.online.attendance.billing;

import com.online.attendance.security.CurrentCompanyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class SubscriptionEnforcementFilter extends OncePerRequestFilter {

    private final CompanySubscriptionRepository subscriptionRepository;
    private final CurrentCompanyService currentCompanyService;

    public SubscriptionEnforcementFilter(CompanySubscriptionRepository subscriptionRepository, CurrentCompanyService currentCompanyService) {
        this.subscriptionRepository = subscriptionRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return true;

        if (path.startsWith("/api/auth/")) return true;
        if (path.startsWith("/api/billing/")) return true;
        if (path.startsWith("/api/pesapal/")) return true;
        if (path.startsWith("/api/system/")) return true;
        if (path.startsWith("/api/public/")) return true;
        if (path.startsWith("/uploads/")) return true;
        if (path.startsWith("/actuator/")) return true;
        if ("/".equals(path) || "/error".equals(path)) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // System admin should never be blocked.
        if (auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()))) {
            filterChain.doFilter(request, response);
            return;
        }

        CompanySubscription sub;
        try {
            Long companyId = currentCompanyService.requireCompanyId(auth);
            sub = (companyId != null) ? subscriptionRepository.findByCompany_Id(companyId).orElse(null) : null;
        } catch (Exception ex) {
            // If we can't resolve company, let existing auth handlers work.
            filterChain.doFilter(request, response);
            return;
        }

        boolean active = sub != null && sub.getStatus() == SubscriptionStatus.ACTIVE && (sub.getEndAt() == null || Instant.now().isBefore(sub.getEndAt()));
        if (!active) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Subscription inactive. Please renew to continue.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
