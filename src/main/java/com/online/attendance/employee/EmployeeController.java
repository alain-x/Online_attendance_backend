package com.online.attendance.employee;

import com.online.attendance.company.Company;
import com.online.attendance.employee.dto.CreateEmployeeRequest;
import com.online.attendance.employee.dto.EmployeeResponse;
import com.online.attendance.employee.dto.UpdateEmployeeRequest;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
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
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentCompanyService currentCompanyService;

    public EmployeeController(EmployeeRepository employeeRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentCompanyService currentCompanyService) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateEmployeeRequest request) {
        Company company = currentCompanyService.requireCompany(authentication);
        if (userRepository.existsByUsernameAndCompanyId(request.getUsername(), company.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role"));
        }

        AppUser user = AppUser.builder()
                .username(request.getUsername())
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
                .user(user)
                .build();

        employee = employeeRepository.save(employee);

        return ResponseEntity.ok(toResponse(employee));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @GetMapping
    public List<EmployeeResponse> list(Authentication authentication) {
        Company company = currentCompanyService.requireCompany(authentication);
        return employeeRepository.findByUserCompanyId(company.getId()).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(Authentication authentication, @PathVariable Long id) {
        Company company = currentCompanyService.requireCompany(authentication);
        Employee employee = employeeRepository.findByIdAndUserCompanyId(id, company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }
        return ResponseEntity.ok(toResponse(employee));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        Company company = currentCompanyService.requireCompany(authentication);
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

        AppUser user = employee.getUser();
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole()));
            } catch (Exception ex) {
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

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id) {
        Company company = currentCompanyService.requireCompany(authentication);
        Employee employee = employeeRepository.findByIdAndUserCompanyId(id, company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }

        AppUser user = employee.getUser();
        employeeRepository.delete(employee);
        userRepository.delete(user);

        return ResponseEntity.noContent().build();
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getDepartment(),
                employee.getMobile(),
                employee.getDesignation(),
                employee.getCategory(),
                employee.getUser().getUsername(),
                employee.getUser().getRole().name()
        );
    }
}
