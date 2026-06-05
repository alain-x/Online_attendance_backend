package com.online.attendance.company;

import com.online.attendance.company.dto.CompanyResponse;
import com.online.attendance.company.dto.CreateCompanyRequest;
import com.online.attendance.company.dto.RegisterCompanyRequest;
import com.online.attendance.company.dto.UpdateCompanyRequest;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
import com.online.attendance.user.dto.UserCompanyContext;
import com.online.attendance.billing.CompanySubscription;
import com.online.attendance.billing.CompanySubscriptionRepository;
import com.online.attendance.billing.SubscriptionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentCompanyService currentCompanyService;
    private final CompanySubscriptionRepository companySubscriptionRepository;

    public CompanyController(CompanyRepository companyRepository, UserRepository userRepository,
                             PasswordEncoder passwordEncoder, CurrentCompanyService currentCompanyService, CompanySubscriptionRepository companySubscriptionRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentCompanyService = currentCompanyService;
        this.companySubscriptionRepository = companySubscriptionRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping
    public List<CompanyResponse> list(Authentication authentication) {
        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        UserCompanyContext ctx = userRepository.findUserCompanyContext(username, companySlug).orElse(null);
        if (ctx == null) {
            return List.of();
        }
        if (ctx.role() == Role.SYSTEM_ADMIN) {
            return enrichLogoUrls(companyRepository.findAllResponses());
        }

        Long companyId = ctx.companyId();
        if (ctx.parentCompanyId() != null) {
            return enrichLogoUrls(companyRepository.findResponseById(companyId).map(List::of).orElse(List.of()));
        }

        List<CompanyResponse> result = new ArrayList<>();
        companyRepository.findResponseById(companyId).ifPresent(result::add);
        result.addAll(companyRepository.findBranchResponsesByParentId(companyId));
        return enrichLogoUrls(result);
    }

    private List<CompanyResponse> enrichLogoUrls(List<CompanyResponse> companies) {
        if (companies == null || companies.isEmpty()) {
            return companies;
        }
        Set<Long> withLogo = new HashSet<>(companyRepository.findIdsWithLogoBytes());
        return companies.stream().map(c -> {
            if (withLogo.contains(c.id())) {
                return new CompanyResponse(
                        c.id(),
                        c.name(),
                        c.slug(),
                        CompanyLogoUrls.apiImagePath(c.id()),
                        c.hourlyRateDefault(),
                        c.active(),
                        c.parentCompanyId()
                );
            }
            return c;
        }).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(Authentication authentication, @PathVariable Long id) {
        CompanyResponse company = companyRepository.findResponseById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canManageCompany(authentication, company.id(), company.parentCompanyId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You can only manage your own company account"));
        }
        return ResponseEntity.ok(company);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id,
                                    @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyResponse summary = companyRepository.findResponseById(id).orElse(null);
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canManageCompany(authentication, summary.id(), summary.parentCompanyId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You can only manage your own company account"));
        }
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
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
            if (v.isBlank()) {
                company.setLogoUrl(null);
                company.setLogoBytes(null);
                company.setLogoContentType(null);
            } else {
                company.setLogoUrl(CompanyLogoUrls.normalizeStoredUrl(v, company.getId()));
            }
        }
        if (request.getHourlyRateDefault() != null) {
            company.setHourlyRateDefault(request.getHourlyRateDefault());
        }
        companyRepository.save(company);
        return companyRepository.findResponseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id) {
        CompanyResponse summary = companyRepository.findResponseById(id).orElse(null);
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canManageCompany(authentication, summary.id(), summary.parentCompanyId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You can only manage your own company account"));
        }
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
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
        companyRepository.save(company);
        return companyRepository.findResponseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean canManageCompany(Authentication authentication, Long companyId, Long parentCompanyId) {
        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        UserCompanyContext ctx = userRepository.findUserCompanyContext(username, companySlug).orElse(null);
        if (ctx == null || ctx.companyId() == null) {
            return false;
        }
        if (ctx.role() == Role.SYSTEM_ADMIN) {
            return true;
        }
        Long currentCompanyId = ctx.companyId();
        if (companyId != null && currentCompanyId.equals(companyId)) {
            return true;
        }
        return parentCompanyId != null && currentCompanyId.equals(parentCompanyId);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping("/{id}/branches")
    public ResponseEntity<?> listBranches(Authentication authentication, @PathVariable Long id) {
        CompanyResponse parent = companyRepository.findResponseById(id).orElse(null);
        if (parent == null) {
            return ResponseEntity.notFound().build();
        }
        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        UserCompanyContext ctx = userRepository.findUserCompanyContext(username, companySlug).orElse(null);
        if (ctx == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Not allowed to list branches of this company"));
        }
        if (ctx.role() != Role.SYSTEM_ADMIN
                && !ctx.companyId().equals(parent.id())
                && (parent.parentCompanyId() == null || !ctx.companyId().equals(parent.parentCompanyId()))) {
            return ResponseEntity.status(403).body(Map.of("message", "Not allowed to list branches of this company"));
        }
        return ResponseEntity.ok(companyRepository.findBranchResponsesByParentId(id));
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

        company = companyRepository.save(company);
        if (companySubscriptionRepository.findByCompany_Id(company.getId()).isEmpty()) {
            companySubscriptionRepository.save(CompanySubscription.builder()
                    .company(company)
                    .status(SubscriptionStatus.INACTIVE)
                    .updatedAt(java.time.Instant.now())
                    .build());
        }
        return ResponseEntity.ok(CompanyResponse.from(company));
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

        if (companySubscriptionRepository.findByCompany_Id(company.getId()).isEmpty()) {
            companySubscriptionRepository.save(CompanySubscription.builder()
                    .company(company)
                    .status(SubscriptionStatus.INACTIVE)
                    .updatedAt(java.time.Instant.now())
                    .build());
        }

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
    @Transactional
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadLogo(Authentication authentication, @PathVariable Long id, @RequestPart("file") @NotNull MultipartFile file) throws IOException {
        CompanyResponse summary = companyRepository.findResponseById(id).orElse(null);
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        if (!canManageCompany(authentication, summary.id(), summary.parentCompanyId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You can only manage your own company account"));
        }
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is required"));
        }

        company.setLogoBytes(file.getBytes());
        company.setLogoContentType(file.getContentType());
        company.setLogoUrl(CompanyLogoUrls.apiImagePath(company.getId()));
        companyRepository.save(company);
        return companyRepository.findResponseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Backward-compatible alias when logoUrl omits /image */
    @Transactional(readOnly = true)
    @GetMapping("/{id}/logo")
    public ResponseEntity<?> getLogo(@PathVariable Long id) {
        return getLogoImage(id);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}/logo/image")
    public ResponseEntity<?> getLogoImage(@PathVariable Long id) {
        try {
            return companyRepository.findLogoViewById(id)
                    .filter(view -> view.getLogoBytes() != null && view.getLogoBytes().length > 0)
                    .map(view -> {
                        String ct = view.getLogoContentType();
                        String contentType = (ct != null && !ct.isBlank()) ? ct : MediaType.APPLICATION_OCTET_STREAM_VALUE;
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.parseMediaType(contentType));
                        headers.setCacheControl("public, max-age=86400");
                        return ResponseEntity.ok().headers(headers).body(view.getLogoBytes());
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            log.error("Error serving logo for company {}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("message", "Failed to serve logo image"));
        }
    }
}
