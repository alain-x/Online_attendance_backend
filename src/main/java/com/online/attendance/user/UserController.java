package com.online.attendance.user;

import com.online.attendance.company.Company;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.user.dto.CreateUserRequest;
import com.online.attendance.user.dto.UpdateUserRequest;
import com.online.attendance.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentCompanyService currentCompanyService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentCompanyService currentCompanyService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping
    public List<UserResponse> list(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        return userRepository.findAllByCompanyId(company.getId()).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateUserRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        if (userRepository.existsByUsernameAndCompanyId(request.getUsername(), company.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role"));
        }

        boolean enabled = request.getEnabled() == null || request.getEnabled();

        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .company(company)
                .enabled(enabled)
                .build();

        user = userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id, @RequestBody UpdateUserRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        AppUser user = userRepository.findByIdAndCompanyId(id, company.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            Role role;
            try {
                role = Role.valueOf(request.getRole());
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid role"));
            }
            user.setRole(role);
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    private UserResponse toResponse(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .enabled(user.isEnabled())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .companySlug(user.getCompany() != null ? user.getCompany().getSlug() : null)
                .build();
    }
}
