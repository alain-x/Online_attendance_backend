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

    private final String systemAdminUsername;
    private final String systemAdminPassword;

    public BootstrapAdminUser(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.username:admin}") String adminUsername,
            @Value("${app.bootstrap.admin.password:admin123}") String adminPassword,
            @Value("${app.bootstrap.systemadmin.username:sysadmin}") String systemAdminUsername,
            @Value("${app.bootstrap.systemadmin.password:sysadmin123}") String systemAdminPassword
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.systemAdminUsername = systemAdminUsername;
        this.systemAdminPassword = systemAdminPassword;
    }

    @Override
    public void run(String... args) {
        Company company = companyRepository.findBySlug("default")
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .name("Default Company")
                        .slug("default")
                        .build()));

        if (!userRepository.existsByUsernameAndCompanyId(adminUsername, company.getId())) {
            AppUser admin = AppUser.builder()
                    .username(adminUsername)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .company(company)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsernameAndCompanyId(systemAdminUsername, company.getId())) {
            AppUser sys = AppUser.builder()
                    .username(systemAdminUsername)
                    .passwordHash(passwordEncoder.encode(systemAdminPassword))
                    .role(Role.SYSTEM_ADMIN)
                    .company(company)
                    .enabled(true)
                    .build();
            userRepository.save(sys);
        }
    }
}
