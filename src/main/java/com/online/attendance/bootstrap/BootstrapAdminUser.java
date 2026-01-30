package com.online.attendance.bootstrap;

import com.online.attendance.company.Company;
import com.online.attendance.company.CompanyRepository;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminUser implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminUsername;
    private final String adminPassword;

    public BootstrapAdminUser(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.username:admin}") String adminUsername,
            @Value("${app.bootstrap.admin.password:admin123}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        Company company = companyRepository.findBySlug("default")
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .name("Default Company")
                        .slug("default")
                        .build()));

        if (userRepository.existsByUsernameAndCompanyId(adminUsername, company.getId())) {
            return;
        }

        AppUser admin = AppUser.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .company(company)
                .enabled(true)
                .build();

        userRepository.save(admin);
    }
}
