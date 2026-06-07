package com.online.attendance.user;

import com.online.attendance.company.Company;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.user.dto.CreateUserRequest;
import com.online.attendance.user.dto.UpdateUserRequest;
import com.online.attendance.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final EmployeeRepository employeeRepository;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentCompanyService currentCompanyService, EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentCompanyService = currentCompanyService;
        this.employeeRepository = employeeRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','CLUB_ADMIN')")
    @GetMapping
    public List<UserResponse> list(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        return userRepository.findAllByCompanyId(company.getId()).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','CLUB_ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateUserRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        String requestedUsername = request.getUsername() != null ? request.getUsername().trim() : "";
        if (userRepository.existsByUsernameAndCompanyId(requestedUsername, company.getId())) {
            return ResponseEntity.status(409).body(Map.of("message", requestedUsername + " is taken, try another one"));
        }

        String requestedEmail = request.getEmail() != null ? request.getEmail().trim() : "";
        if (!requestedEmail.isBlank() && userRepository.existsByEmailAndCompanyId(requestedEmail, company.getId())) {
            return ResponseEntity.status(409).body(Map.of("message", "Email already exists"));
        }

        Role role;
        try {
            role = Role.fromString(request.getRole())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid role"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role"));
        }

        boolean enabled = request.getEnabled() == null || request.getEnabled();

        AppUser user = AppUser.builder()
                .username(requestedUsername)
                .email(requestedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .company(company)
                .enabled(enabled)
                .build();

        try {
            user = userRepository.save(user);
            return ResponseEntity.ok(toResponse(user));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).body(Map.of("message", requestedUsername + " is taken, try another one"));
        }
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','CLUB_ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody UpdateUserRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        AppUser user = userRepository.findByIdAndCompanyId(id, company.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            Role role;
            try {
                role = Role.fromString(request.getRole())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid role"));
            } catch (IllegalArgumentException ex) {
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

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String requestedEmail = request.getEmail().trim();
            String currentEmail = user.getEmail() != null ? user.getEmail().trim() : "";
            if (!requestedEmail.equalsIgnoreCase(currentEmail)) {
                if (userRepository.existsByEmailAndCompanyId(requestedEmail, company.getId())) {
                    return ResponseEntity.status(409).body(Map.of("message", "Email already exists"));
                }
                user.setEmail(requestedEmail);
            }
        }

        user = userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','CLUB_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        AppUser user = userRepository.findByIdAndCompanyId(id, company.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        // In this system, employee accounts are linked one-to-one with users.
        // To avoid orphaning employees and related attendance records, require deletion through Staff/Employee deletion.
        if (employeeRepository.findByUserIdAndUserCompanyId(user.getId(), company.getId()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "User is linked to an employee. Delete the employee from Settings instead."));
        }

        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .enabled(user.isEnabled())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .companySlug(user.getCompany() != null ? user.getCompany().getSlug() : null)
                .build();
    }
}
