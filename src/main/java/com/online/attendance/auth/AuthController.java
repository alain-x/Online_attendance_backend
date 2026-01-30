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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CurrentCompanyService currentCompanyService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository, CurrentCompanyService currentCompanyService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String principal = request.getCompanySlug() + "::" + request.getUsername();
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(principal, request.getPassword())
        );

        String role = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse("ROLE_EMPLOYEE");

        AppUser user = userRepository.findByUsernameAndCompanySlug(request.getUsername(), request.getCompanySlug()).orElse(null);
        Long companyId = user != null && user.getCompany() != null ? user.getCompany().getId() : null;
        String companySlug = user != null && user.getCompany() != null ? user.getCompany().getSlug() : null;

        String token = jwtService.generateToken(
                principal,
                Map.of(
                        "role", role,
                        "companyId", companyId,
                        "companySlug", companySlug
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
                "companySlug", user.getCompany() != null ? user.getCompany().getSlug() : null
        ));
    }
}
