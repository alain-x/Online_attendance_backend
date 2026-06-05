package com.online.attendance.face;

import com.online.attendance.audit.AuditService;
import com.online.attendance.company.Company;
import com.online.attendance.employee.Employee;
import com.online.attendance.employee.EmployeeProfileImageService;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.employee.dto.EmployeeResponse;
import com.online.attendance.security.CurrentCompanyService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/face")
public class FaceController {

    private final EmployeeRepository employeeRepository;
    private final FaceService faceService;
    private final OpenCvImageQualityService openCvImageQualityService;
    private final CurrentCompanyService currentCompanyService;
    private final AuditService auditService;
    private final EmployeeProfileImageService employeeProfileImageService;

    public FaceController(
            EmployeeRepository employeeRepository,
            FaceService faceService,
            OpenCvImageQualityService openCvImageQualityService,
            CurrentCompanyService currentCompanyService,
            AuditService auditService,
            EmployeeProfileImageService employeeProfileImageService
    ) {
        this.employeeRepository = employeeRepository;
        this.faceService = faceService;
        this.openCvImageQualityService = openCvImageQualityService;
        this.currentCompanyService = currentCompanyService;
        this.auditService = auditService;
        this.employeeProfileImageService = employeeProfileImageService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','EMPLOYEE', 'ADMIN')")
    @PostMapping(value = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> enroll(
            Authentication authentication,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "descriptor", required = false) String descriptorJson,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Employee employee = employeeRepository.findByUserUsernameAndUserCompanyId(currentCompanyService.requireUsername(authentication), company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee profile not found"));
        }

        if (image != null && !image.isEmpty()) {
            String qualityError = openCvImageQualityService.validate(image);
            if (qualityError != null) {
                return ResponseEntity.badRequest().body(Map.of("message", qualityError));
            }
        }

        if (descriptorJson == null || descriptorJson.isBlank()) {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message",
                        "Face descriptor missing. Please ensure the AI models are installed (public/models) and try enrolling again with a clear face photo."
                ));
            }
            employee.setFaceTemplateRef(faceService.hash(image));
        } else {
            employee.setFaceDescriptor(descriptorJson);
            if (image != null && !image.isEmpty()) {
                employee.setFaceTemplateRef(faceService.hash(image));
            }
        }

        if (image != null && !image.isEmpty()) {
            try {
                employeeProfileImageService.saveProfileImage(employee, image);
            } catch (Exception ignored) {
            }
        }
        employee = employeeRepository.save(employee);

        auditService.log(
                company.getId(),
                currentCompanyService.requireUsername(authentication),
                "FACE_ENROLL",
                "Employee",
                employee.getId(),
                null
        );

        // Return updated employee profile with image URL
        return ResponseEntity.ok(toEmployeeResponse(employee));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','RECORDER')")
    @PostMapping(value = "/enroll/{employeeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> enrollForEmployee(
            Authentication authentication,
            @PathVariable("employeeId") Long employeeId,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "descriptor", required = false) String descriptorJson,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Employee employee = employeeRepository.findByIdAndUserCompanyId(employeeId, company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }

        if (image != null && !image.isEmpty()) {
            String qualityError = openCvImageQualityService.validate(image);
            if (qualityError != null) {
                return ResponseEntity.badRequest().body(Map.of("message", qualityError));
            }
        }

        if (descriptorJson == null || descriptorJson.isBlank()) {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message",
                        "Face descriptor missing. Please ensure the AI models are installed (public/models) and try enrolling again with a clear face photo."
                ));
            }
            employee.setFaceTemplateRef(faceService.hash(image));
        } else {
            employee.setFaceDescriptor(descriptorJson);
            if (image != null && !image.isEmpty()) {
                employee.setFaceTemplateRef(faceService.hash(image));
            }
        }

        if (image != null && !image.isEmpty()) {
            try {
                employeeProfileImageService.saveProfileImage(employee, image);
            } catch (Exception ignored) {
            }
        }
        employee = employeeRepository.save(employee);

        auditService.log(
                company.getId(),
                currentCompanyService.requireUsername(authentication),
                "FACE_ENROLL",
                "Employee",
                employee.getId(),
                null
        );

        // Return updated employee profile with image URL
        return ResponseEntity.ok(toEmployeeResponse(employee));
    }

    private EmployeeResponse toEmployeeResponse(Employee employee) {
        String profileUrl = resolveProfileImageUrl(employee);
        boolean faceEnrolled = employee.getFaceDescriptor() != null && !employee.getFaceDescriptor().isBlank();
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getDepartment(),
                employee.getMobile(),
                employee.getDesignation(),
                employee.getCategory(),
                profileUrl,
                employee.getUser().getUsername(),
                employee.getUser().getEmail(),
                employee.getUser().getRole().name(),
                faceEnrolled,
                employee.getHourlyRateOverride()
        );
    }

    private String resolveProfileImageUrl(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return null;
        }
        return employeeRepository.findProfileImageById(employee.getId())
                .filter(view -> view.getProfileImageBytes() != null && view.getProfileImageBytes().length > 0)
                .map(view -> EmployeeProfileImageService.profileImageApiUrl(employee.getId()))
                .orElseGet(() -> {
                    String url = employee.getProfileImageUrl();
                    if (url != null && (url.startsWith("/uploads/") || url.startsWith("uploads/"))) {
                        return EmployeeProfileImageService.profileImageApiUrl(employee.getId());
                    }
                    return url;
                });
    }

}
