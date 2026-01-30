package com.online.attendance.face;

import com.online.attendance.audit.AuditService;
import com.online.attendance.company.Company;
import com.online.attendance.employee.Employee;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.security.CurrentCompanyService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/face")
public class FaceController {

    private final EmployeeRepository employeeRepository;
    private final FaceService faceService;
    private final CurrentCompanyService currentCompanyService;
    private final AuditService auditService;

    public FaceController(
            EmployeeRepository employeeRepository,
            FaceService faceService,
            CurrentCompanyService currentCompanyService,
            AuditService auditService
    ) {
        this.employeeRepository = employeeRepository;
        this.faceService = faceService;
        this.currentCompanyService = currentCompanyService;
        this.auditService = auditService;
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping(value = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> enroll(Authentication authentication, @RequestPart("image") @NotNull MultipartFile image) {
        Company company = currentCompanyService.requireCompany(authentication);
        Employee employee = employeeRepository.findByUserUsernameAndUserCompanyId(currentCompanyService.requireUsername(authentication), company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee profile not found"));
        }

        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Image is required"));
        }

        employee.setFaceTemplateRef(faceService.hash(image));
        employeeRepository.save(employee);

        auditService.log(
                company.getId(),
                currentCompanyService.requireUsername(authentication),
                "FACE_ENROLL",
                "Employee",
                employee.getId(),
                null
        );

        return ResponseEntity.ok(Map.of("message", "Face enrolled"));
    }
}
