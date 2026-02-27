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
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
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

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','EMPLOYEE', 'ADMIN')")
    @PostMapping(value = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
                saveEmployeeProfileImage(company.getId(), employee, image);
            } catch (Exception ignored) {
            }
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

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','RECORDER')")
    @PostMapping(value = "/enroll/{employeeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
                saveEmployeeProfileImage(company.getId(), employee, image);
            } catch (Exception ignored) {
            }
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

    private void saveEmployeeProfileImage(Long companyId, Employee employee, MultipartFile image) throws IOException {
        if (employee == null || employee.getId() == null || image == null || image.isEmpty()) {
            return;
        }

        Path dir = Paths.get("uploads", "profile-images", String.valueOf(companyId != null ? companyId : 0L));
        Files.createDirectories(dir);

        String ext = ".jpg";
        String original = image.getOriginalFilename();
        if (original != null) {
            int idx = original.lastIndexOf('.');
            if (idx >= 0 && idx < original.length() - 1) {
                String candidate = original.substring(idx);
                if (candidate.length() <= 10) {
                    ext = candidate;
                }
            }
        }

        String filename = "emp-" + employee.getId() + "-" + UUID.randomUUID() + ext;
        Path out = dir.resolve(filename);
        Files.write(out, image.getBytes());

        String prevPath = employee.getProfileImagePath();
        if (prevPath != null && !prevPath.isBlank()) {
            try {
                Files.deleteIfExists(Paths.get(prevPath));
            } catch (Exception ignored) {
            }
        }

        employee.setProfileImagePath(out.toString());
        employee.setProfileImageUrl("/uploads/profile-images/" + (companyId != null ? companyId : 0L) + "/" + filename);
    }
}
