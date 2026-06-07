package com.online.attendance.user;

import com.online.attendance.company.Company;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.user.dto.CreateUserRequest;
import com.online.attendance.user.dto.UpdateUserRequest;
import com.online.attendance.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Transactional(readOnly = true)
public class UserController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserController.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentCompanyService currentCompanyService;
    private final EmployeeRepository employeeRepository;
    private final UserProfileImageService userProfileImageService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentCompanyService currentCompanyService, EmployeeRepository employeeRepository, UserProfileImageService userProfileImageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentCompanyService = currentCompanyService;
        this.employeeRepository = employeeRepository;
        this.userProfileImageService = userProfileImageService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','CLUB_ADMIN')")
    @GetMapping
    public List<UserResponse> list(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        return userRepository.findAllByCompanyId(company.getId()).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','CLUB_ADMIN')")
    @PostMapping
    @Transactional
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

        String firstName = request.getFirstName() != null ? request.getFirstName().trim() : "";
        String lastName = request.getLastName() != null ? request.getLastName().trim() : "";

        AppUser user = AppUser.builder()
                .username(requestedUsername)
                .firstName(firstName.isBlank() ? null : firstName)
                .lastName(lastName.isBlank() ? null : lastName)
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
    @Transactional
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

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim().isBlank() ? null : request.getFirstName().trim());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim().isBlank() ? null : request.getLastName().trim());
        }

        user = userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','CLUB_ADMIN')")
    @DeleteMapping("/{id}")
    @Transactional
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

    @PreAuthorize("isAuthenticated()")
    @Transactional
    @PostMapping(value = "/me/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMyProfileImage(Authentication authentication, @RequestParam("image") MultipartFile image) {
        AppUser user = requireCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Image file is required"));
        }
        try {
            userProfileImageService.saveProfileImage(user, image);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile image updated",
                    "profileImageUrl", "/api/users/" + user.getId() + "/profile/image"
            ));
        } catch (Exception ex) {
            log.error("Failed to save profile image for user {}: {}", user.getId(), ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("message", "Failed to save profile image: " + ex.getMessage()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    @DeleteMapping("/me/profile/image")
    public ResponseEntity<?> deleteMyProfileImage(Authentication authentication) {
        AppUser user = requireCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        user.setProfileImageBytes(null);
        user.setProfileImageContentType(null);
        user.setProfileImagePath(null);
        user.setProfileImageUrl(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile image removed"));
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}/profile/image")
    public ResponseEntity<?> profileImage(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean download) {
        try {
            Optional<UserRepository.UserProfileImageView> viewOpt = userRepository.findProfileImageById(id)
                    .filter(view -> view.getProfileImageBytes() != null && view.getProfileImageBytes().length > 0);
            if (viewOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "Profile image not found"));
            }
            UserRepository.UserProfileImageView view = viewOpt.get();
            String contentType = view.getProfileImageContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setCacheControl("public, max-age=86400");
            if (download) {
                headers.setContentDisposition(
                        org.springframework.http.ContentDisposition.attachment().filename("profile-" + id + ".jpg").build()
                );
            }
            return ResponseEntity.ok().headers(headers).body(view.getProfileImageBytes());
        } catch (Exception ex) {
            log.error("Error serving profile image for user {}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("message", "Failed to serve profile image"));
        }
    }

    private AppUser requireCurrentUser(Authentication authentication) {
        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        return userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
    }

    private UserResponse toResponse(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .enabled(user.isEnabled())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .companySlug(user.getCompany() != null ? user.getCompany().getSlug() : null)
                .build();
    }
}
