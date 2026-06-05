package com.online.attendance.employee;

import com.online.attendance.company.Company;
import com.online.attendance.employee.dto.CreateEmployeeRequest;
import com.online.attendance.employee.dto.EmployeeResponse;
import com.online.attendance.employee.dto.UpdateEmployeeRequest;
import com.online.attendance.employee.dto.UpdateMyProfileRequest;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentCompanyService currentCompanyService;
    private final EmployeeProfileImageService employeeProfileImageService;

    public EmployeeController(EmployeeRepository employeeRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentCompanyService currentCompanyService, EmployeeProfileImageService employeeProfileImageService) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentCompanyService = currentCompanyService;
        this.employeeProfileImageService = employeeProfileImageService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateEmployeeRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
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

        AppUser user = AppUser.builder()
                .username(requestedUsername)
                .email(requestedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .company(company)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        Employee employee = Employee.builder()
                .employeeCode(request.getEmployeeCode())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .department(request.getDepartment())
                .mobile(request.getMobile())
                .designation(request.getDesignation())
                .category(request.getCategory())
                .hourlyRateOverride(request.getHourlyRateOverride())
                .user(user)
                .build();

        employee = employeeRepository.save(employee);

        return ResponseEntity.ok(toResponse(employee));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','RECORDER')")
    @GetMapping
    public List<EmployeeResponse> list(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        return employeeRepository.findByUserCompanyId(company.getId()).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','RECORDER')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Employee employee = employeeRepository.findByIdAndUserCompanyId(id, company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }
        return ResponseEntity.ok(toResponse(employee));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Employee employee = employeeRepository.findByIdAndUserCompanyId(id, company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setDepartment(request.getDepartment());
        employee.setMobile(request.getMobile());
        employee.setDesignation(request.getDesignation());
        employee.setCategory(request.getCategory());
        if (request.getHourlyRateOverride() != null) {
            employee.setHourlyRateOverride(request.getHourlyRateOverride());
        }

        AppUser user = employee.getUser();

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String requestedUsername = request.getUsername().trim();
            String currentUsername = user.getUsername();
            if (!requestedUsername.equalsIgnoreCase(currentUsername)) {
                if (userRepository.existsByUsernameAndCompanyId(requestedUsername, company.getId())) {
                    return ResponseEntity.status(409).body(Map.of("message", requestedUsername + " is taken, try another one"));
                }
                user.setUsername(requestedUsername);
            }
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

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.fromString(request.getRole())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid role")));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid role"));
            }
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        userRepository.save(user);
        employee = employeeRepository.save(employee);

        return ResponseEntity.ok(toResponse(employee));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Employee employee = employeeRepository.findByIdAndUserCompanyId(id, company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }

        AppUser user = employee.getUser();
        employeeRepository.delete(employee);
        userRepository.delete(user);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<?> myProfile(Authentication authentication) {
        Employee employee = requireCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee profile not found"));
        }
        return ResponseEntity.ok(toResponse(employee));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(Authentication authentication, @Valid @RequestBody UpdateMyProfileRequest request) {
        Employee employee = requireCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee profile not found"));
        }
        if (request.getMobile() != null) {
            employee.setMobile(request.getMobile().trim().isBlank() ? null : request.getMobile().trim());
        }
        if (request.getDepartment() != null) {
            employee.setDepartment(request.getDepartment().trim().isBlank() ? null : request.getDepartment().trim());
        }
        if (request.getDesignation() != null) {
            employee.setDesignation(request.getDesignation().trim().isBlank() ? null : request.getDesignation().trim());
        }
        if (request.getCategory() != null) {
            employee.setCategory(request.getCategory().trim().isBlank() ? null : request.getCategory().trim());
        }
        employee = employeeRepository.save(employee);
        return ResponseEntity.ok(toResponse(employee));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @GetMapping("/me/profile/image")
    public ResponseEntity<?> myProfileImage(Authentication authentication, @RequestParam(defaultValue = "false") boolean download) {
        Employee employee = requireCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Profile image not found"));
        }
        return profileImageResponse(employee.getId(), download);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    @PutMapping(value = "/me/profile/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PostMapping(value = "/me/profile/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMyProfileImage(Authentication authentication, org.springframework.web.multipart.MultipartFile image) {
        Employee employee = requireCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee profile not found"));
        }
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Image file is required"));
        }
        try {
            employeeProfileImageService.saveProfileImage(employee, image);
            employeeRepository.save(employee);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile image updated",
                    "profileImageUrl", EmployeeProfileImageService.profileImageApiUrl(employee.getId())
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to save profile image: " + ex.getMessage()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me/profile/image")
    public ResponseEntity<?> deleteMyProfileImage(Authentication authentication) {
        Employee employee = requireCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee profile not found"));
        }
        employee.setProfileImageBytes(null);
        employee.setProfileImageContentType(null);
        employee.setProfileImagePath(null);
        employee.setProfileImageUrl(null);
        employeeRepository.save(employee);
        return ResponseEntity.ok(Map.of("message", "Profile image removed"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/profile/image-url")
    public ResponseEntity<?> myProfileImageUrl(Authentication authentication) {
        Employee employee = requireCurrentEmployee(authentication);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee profile not found"));
        }
        return ResponseEntity.ok(Map.of("profileImageUrl", EmployeeProfileImageService.profileImageApiUrl(employee.getId())));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @GetMapping("/{id}/profile/image")
    public ResponseEntity<?> profileImage(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId
    ) {
        Long resolvedCompanyId = currentCompanyService.requireCompanyId(authentication, companyId);
        Employee employee = employeeRepository.findByIdAndUserCompanyId(id, resolvedCompanyId).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }
        return profileImageResponse(id, download);
    }

    private Employee requireCurrentEmployee(Authentication authentication) {
        String username = currentCompanyService.requireUsername(authentication);
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        return employeeRepository.findByUserUsernameAndUserCompanyId(username, companyId).orElse(null);
    }

    private ResponseEntity<?> profileImageResponse(Long employeeId, boolean download) {
        try {
            Optional<EmployeeRepository.ProfileImageView> viewOpt = employeeRepository.findProfileImageById(employeeId)
                    .filter(view -> view.getProfileImageBytes() != null && view.getProfileImageBytes().length > 0);
            if (viewOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "Profile image not found"));
            }
            EmployeeRepository.ProfileImageView view = viewOpt.get();
            String contentType = view.getProfileImageContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setCacheControl("public, max-age=86400");
            if (download) {
                headers.setContentDisposition(
                        ContentDisposition.attachment().filename("profile-" + employeeId + ".jpg").build()
                );
            }
            return ResponseEntity.ok().headers(headers).body(view.getProfileImageBytes());
        } catch (Exception ex) {
            log.error("Error serving profile image for employee {}: {}", employeeId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("message", "Failed to serve profile image"));
        }
    }

    private EmployeeResponse toResponse(Employee employee) {
        String profileUrl = resolveProfileImageUrl(employee);
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getDepartment(),
                employee.getMobile(),
                employee.getDesignation(),
                employee.getCategory(),
                profileUrl,
                employee.getUser().getUsername(),
                employee.getUser().getEmail(),
                employee.getUser().getRole().name(),
                employee.getHourlyRateOverride()
        );
    }

    private String resolveProfileImageUrl(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return null;
        }
        return employeeRepository.findProfileImageById(employee.getId())
                .filter(view -> view.getProfileImageBytes() != null && view.getProfileImageBytes().length > 0)
                .map(view -> EmployeeProfileImageService.profileImageApiUrl(employee.getId()))
                .orElseGet(() -> {
                    String url = employee.getProfileImageUrl();
                    if (url != null && (url.startsWith("/uploads/") || url.startsWith("uploads/"))) {
                        return EmployeeProfileImageService.profileImageApiUrl(employee.getId());
                    }
                    return url;
                });
    }
}
