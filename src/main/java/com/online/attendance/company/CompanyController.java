package com.online.attendance.company;

import com.online.attendance.company.dto.CreateCompanyRequest;
import com.online.attendance.company.dto.RegisterCompanyRequest;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CompanyController(CompanyRepository companyRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping
    public List<Company> list() {
        return companyRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateCompanyRequest request) {
        if (companyRepository.existsBySlug(request.getSlug())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Company slug already exists"));
        }

        Company company = Company.builder()
                .name(request.getName())
                .slug(request.getSlug())
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
}
