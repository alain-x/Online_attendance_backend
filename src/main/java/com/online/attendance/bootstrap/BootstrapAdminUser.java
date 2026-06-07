package com.online.attendance.bootstrap;

import com.online.attendance.company.Company;
import com.online.attendance.company.CompanyRepository;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class BootstrapAdminUser implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminUsername;
    private final String adminPassword;

    private final String systemAdminUsername;
    private final String systemAdminPassword;

    private final String clubAdminUsername;
    private final String clubAdminPassword;

    public BootstrapAdminUser(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.username:admin}") String adminUsername,
            @Value("${app.bootstrap.admin.password:admin123}") String adminPassword,
            @Value("${app.bootstrap.systemadmin.username:sysadmin}") String systemAdminUsername,
            @Value("${app.bootstrap.systemadmin.password:sysadmin123}") String systemAdminPassword,
            @Value("${app.bootstrap.clubadmin.username:clubadmin}") String clubAdminUsername,
            @Value("${app.bootstrap.clubadmin.password:clubadmin123}") String clubAdminPassword
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.systemAdminUsername = systemAdminUsername;
        this.systemAdminPassword = systemAdminPassword;
        this.clubAdminUsername = clubAdminUsername;
        this.clubAdminPassword = clubAdminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Company company = companyRepository.findBySlug("default")
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .name("Default Company")
                        .slug("default")
                        .build()));

        // Ensure bootstrap users always match configured credentials (useful in deployments)
        AppUser admin = userRepository.findByUsernameAndCompanySlug(adminUsername, company.getSlug()).orElse(null);
        if (admin == null) {
            admin = AppUser.builder()
                    .username(adminUsername)
                    .firstName("Admin")
                    .lastName("User")
                    .role(Role.ADMIN)
                    .company(company)
                    .enabled(true)
                    .build();
        }
        admin.setEnabled(true);
        admin.setRole(Role.ADMIN);
        admin.setCompany(company);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        userRepository.save(admin);

        AppUser sys = userRepository.findByUsernameAndCompanySlug(systemAdminUsername, company.getSlug()).orElse(null);
        if (sys == null) {
            sys = AppUser.builder()
                    .username(systemAdminUsername)
                    .firstName("System")
                    .lastName("Admin")
                    .role(Role.SYSTEM_ADMIN)
                    .company(company)
                    .enabled(true)
                    .build();
        }
        sys.setEnabled(true);
        sys.setRole(Role.SYSTEM_ADMIN);
        sys.setCompany(company);
        sys.setPasswordHash(passwordEncoder.encode(systemAdminPassword));
        userRepository.save(sys);

        // Sports Club administrator — default user for day-to-day sports club management
        AppUser club = userRepository.findByUsernameAndCompanySlug(clubAdminUsername, company.getSlug()).orElse(null);
        if (club == null) {
            club = AppUser.builder()
                    .username(clubAdminUsername)
                    .firstName("Club")
                    .lastName("Admin")
                    .role(Role.CLUB_ADMIN)
                    .company(company)
                    .enabled(true)
                    .build();
        }
        club.setEnabled(true);
        club.setRole(Role.CLUB_ADMIN);
        club.setCompany(company);
        club.setPasswordHash(passwordEncoder.encode(clubAdminPassword));
        userRepository.save(club);
    }
}
