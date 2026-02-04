package com.online.attendance.auth;

import com.online.attendance.auth.dto.LoginRequest;
import com.online.attendance.auth.dto.LoginResponse;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.security.JwtService;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CurrentCompanyService currentCompanyService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository, CurrentCompanyService currentCompanyService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.currentCompanyService = currentCompanyService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword();
        String companySlugParam = request.getCompanySlug() != null ? request.getCompanySlug().trim() : "";

        if (username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        AppUser user;

        if (!companySlugParam.isEmpty()) {
            // Company slug provided: use existing flow (company::username)
            String principal = companySlugParam + "::" + username;
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(principal, password)
                );
            } catch (Exception ex) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
            }
            user = userRepository.findByUsernameAndCompanySlug(username, companySlugParam).orElse(null);
        } else {
            // No company slug: find user by username across all companies and verify password
            List<AppUser> candidates = userRepository.findAllByUsername(username);
            user = null;
            for (AppUser u : candidates) {
                if (u.isEnabled() && passwordEncoder.matches(password, u.getPasswordHash())) {
                    user = u;
                    break;
                }
            }
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
            }
        }

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        if (user.getCompany() != null && !user.getCompany().isActive()) {
            return ResponseEntity.status(403).body(Map.of("message", "Company account is inactive. Please contact system administrator."));
        }

        String principal = (user.getCompany() != null ? user.getCompany().getSlug() : "default") + "::" + user.getUsername();
        String role = "ROLE_" + user.getRole().name();
        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        String companySlug = user.getCompany() != null ? user.getCompany().getSlug() : null;

        String token = jwtService.generateToken(
                principal,
                Map.of(
                        "role", role,
                        "companyId", companyId != null ? companyId : 0L,
                        "companySlug", companySlug != null ? companySlug : "default"
                )
        );

        return ResponseEntity.ok(new LoginResponse(token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser user = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "companyId", user.getCompany() != null ? user.getCompany().getId() : null,
                "companySlug", user.getCompany() != null ? user.getCompany().getSlug() : null,
                "companyLogoUrl", user.getCompany() != null ? user.getCompany().getLogoUrl() : null
        ));
    }
}
