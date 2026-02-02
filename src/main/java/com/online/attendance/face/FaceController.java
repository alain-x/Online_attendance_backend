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
    private final OpenCvImageQualityService openCvImageQualityService;
    private final CurrentCompanyService currentCompanyService;
    private final AuditService auditService;

    public FaceController(
            EmployeeRepository employeeRepository,
            FaceService faceService,
            OpenCvImageQualityService openCvImageQualityService,
            CurrentCompanyService currentCompanyService,
            AuditService auditService
    ) {
        this.employeeRepository = employeeRepository;
        this.faceService = faceService;
        this.openCvImageQualityService = openCvImageQualityService;
        this.currentCompanyService = currentCompanyService;
        this.auditService = auditService;
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    @PostMapping(value = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> enroll(
            Authentication authentication,
            @RequestPart("image") @NotNull MultipartFile image,
            @RequestPart(value = "descriptor", required = false) String descriptorJson) {
        Company company = currentCompanyService.requireCompany(authentication);
        Employee employee = employeeRepository.findByUserUsernameAndUserCompanyId(currentCompanyService.requireUsername(authentication), company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee profile not found"));
        }

        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Image is required"));
        }

        String qualityError = openCvImageQualityService.validate(image);
        if (qualityError != null) {
            return ResponseEntity.badRequest().body(Map.of("message", qualityError));
        }

        employee.setFaceTemplateRef(faceService.hash(image));
        if (descriptorJson != null && !descriptorJson.isBlank()) {
            employee.setFaceDescriptor(descriptorJson);
        }
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
