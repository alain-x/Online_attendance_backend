package com.online.attendance.company;

import com.online.attendance.company.dto.CreateCompanyRequest;
import com.online.attendance.company.dto.RegisterCompanyRequest;
import com.online.attendance.company.dto.UpdateCompanyRequest;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentCompanyService currentCompanyService;

    public CompanyController(CompanyRepository companyRepository, UserRepository userRepository,
                             PasswordEncoder passwordEncoder, CurrentCompanyService currentCompanyService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping
    public List<Company> list(Authentication authentication) {
        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser currentUser = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (currentUser != null && currentUser.getRole() == Role.SYSTEM_ADMIN) {
            return companyRepository.findAll();
        }

        Company current = currentCompanyService.requireCompany(authentication);
        // Branch admins see only their own company. Parent admins see their company + direct branches.
        if (current.getParentCompanyId() != null) {
            return List.of(current);
        }

        List<Company> result = new ArrayList<>();
        result.add(current);
        result.addAll(companyRepository.findByParentCompany_Id(current.getId()));
        return result;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(Authentication authentication, @PathVariable Long id) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canManageCompany(authentication, company)) {
            return ResponseEntity.status(403).body(Map.of("message", "You can only manage your own company account"));
        }
        return ResponseEntity.ok(company);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id,
                                    @Valid @RequestBody UpdateCompanyRequest request) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canManageCompany(authentication, company)) {
            return ResponseEntity.status(403).body(Map.of("message", "You can only manage your own company account"));
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            company.setName(request.getName().trim());
        }
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String slug = request.getSlug().trim().toLowerCase().replaceAll("\\s+", "-");
            if (companyRepository.existsBySlugAndIdNot(slug, id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Company slug already exists"));
            }
            company.setSlug(slug);
        }
        if (request.getLogoUrl() != null) {
            String v = request.getLogoUrl().trim();
            company.setLogoUrl(v.isBlank() ? null : v);
            company.setLogoBytes(null);
            company.setLogoContentType(null);
        }
        if (request.getHourlyRateDefault() != null) {
            company.setHourlyRateDefault(request.getHourlyRateDefault());
        }
        company = companyRepository.save(company);
        return ResponseEntity.ok(company);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canManageCompany(authentication, company)) {
            return ResponseEntity.status(403).body(Map.of("message", "You can only manage your own company account"));
        }
        long userCount = userRepository.countByCompanyId(id);
        if (userCount > 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Cannot delete company that has users. Remove or reassign users first."));
        }
        companyRepository.delete(company);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PutMapping("/{id}/active")
    public ResponseEntity<?> setActive(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        Object activeObj = body != null ? body.get("active") : null;
        if (!(activeObj instanceof Boolean)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Field 'active' (boolean) is required"));
        }
        company.setActive((Boolean) activeObj);
        company = companyRepository.save(company);
        return ResponseEntity.ok(company);
    }

    private boolean canManageCompany(Authentication authentication, Company company) {
        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser currentUser = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == Role.SYSTEM_ADMIN) {
            return true;
        }
        if (currentUser.getCompany() == null || currentUser.getCompany().getId() == null) {
            return false;
        }
        Long currentCompanyId = currentUser.getCompany().getId();
        if (company.getId() != null && currentCompanyId.equals(company.getId())) {
            return true;
        }
        Long parentCompanyId = company.getParentCompanyId();
        return parentCompanyId != null && currentCompanyId.equals(parentCompanyId);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping("/{id}/branches")
    public ResponseEntity<?> listBranches(Authentication authentication, @PathVariable Long id) {
        Company parent = companyRepository.findById(id).orElse(null);
        if (parent == null) {
            return ResponseEntity.notFound().build();
        }
        Company current = currentCompanyService.requireCompany(authentication);
        if (!current.getId().equals(parent.getId()) && (current.getParentCompany() == null || !current.getParentCompany().getId().equals(parent.getId()))) {
            return ResponseEntity.status(403).body(Map.of("message", "Not allowed to list branches of this company"));
        }
        return ResponseEntity.ok(companyRepository.findByParentCompany_Id(id));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateCompanyRequest request) {
        if (companyRepository.existsBySlug(request.getSlug())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Company slug already exists"));
        }

        Company parent = null;
        if (request.getParentCompanyId() != null) {
            parent = companyRepository.findById(request.getParentCompanyId()).orElse(null);
            if (parent == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Parent company not found"));
            }
            Company current = currentCompanyService.requireCompany(authentication);
            if (!current.getId().equals(parent.getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "You can only create branches under your own company"));
            }
        }

        Company company = Company.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .logoUrl(request.getLogoUrl() != null && !request.getLogoUrl().trim().isBlank() ? request.getLogoUrl().trim() : null)
                .parentCompany(parent)
                .build();

        return ResponseEntity.ok(companyRepository.save(company));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterCompanyRequest request) {
        if (companyRepository.existsBySlug(request.getCompanySlug())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Company slug already exists"));
        }

        Company company = Company.builder()
                .name(request.getCompanyName())
                .slug(request.getCompanySlug())
                .build();
        company = companyRepository.save(company);

        AppUser admin = AppUser.builder()
                .username(request.getAdminUsername())
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .role(Role.ADMIN)
                .enabled(true)
                .company(company)
                .build();

        userRepository.save(admin);

        return ResponseEntity.ok(Map.of(
                "companyId", company.getId(),
                "companySlug", company.getSlug(),
                "adminUsername", admin.getUsername()
        ));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadLogo(Authentication authentication, @PathVariable Long id, @RequestPart("file") @NotNull MultipartFile file) throws IOException {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canManageCompany(authentication, company)) {
            return ResponseEntity.status(403).body(Map.of("message", "You can only manage your own company account"));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is required"));
        }

        company.setLogoBytes(file.getBytes());
        company.setLogoContentType(file.getContentType());
        company.setLogoUrl("/api/companies/" + company.getId() + "/logo/image");
        company = companyRepository.save(company);
        return ResponseEntity.ok(company);
    }

    @GetMapping("/{id}/logo/image")
    public ResponseEntity<?> getLogoImage(@PathVariable Long id) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null || company.getLogoBytes() == null || company.getLogoBytes().length == 0) {
            return ResponseEntity.notFound().build();
        }
        String ct = company.getLogoContentType();
        String contentType = (ct != null && !ct.isBlank()) ? ct : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        return ResponseEntity.ok().headers(headers).body(company.getLogoBytes());
    }
}
