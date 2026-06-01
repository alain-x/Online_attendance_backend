package com.online.attendance.attendance;

import com.online.attendance.attendance.dto.AttendanceResponse;
import com.online.attendance.attendance.dto.CheckInRequest;
import com.online.attendance.attendance.dto.CheckOutRequest;
import com.online.attendance.attendance.dto.BulkTimesheetImportRequest;
import com.online.attendance.attendance.dto.BulkTimesheetImportResponse;
import com.online.attendance.audit.AuditService;
import com.online.attendance.company.Company;
import com.online.attendance.employee.Employee;
import com.online.attendance.employee.EmployeeProfileImageService;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.face.FaceService;
import com.online.attendance.face.OpenCvImageQualityService;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.location.LocationVerificationService;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final BreakRepository breakRepository;
    private final EmployeeRepository employeeRepository;
    private final LocationVerificationService locationVerificationService;
    private final FaceService faceService;
    private final OpenCvImageQualityService openCvImageQualityService;
    private final CurrentCompanyService currentCompanyService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final EmployeeProfileImageService employeeProfileImageService;

    public AttendanceController(
            AttendanceRepository attendanceRepository,
            BreakRepository breakRepository,
            EmployeeRepository employeeRepository,
            LocationVerificationService locationVerificationService,
            FaceService faceService,
            OpenCvImageQualityService openCvImageQualityService,
            CurrentCompanyService currentCompanyService,
            AuditService auditService,
            UserRepository userRepository,
            EmployeeProfileImageService employeeProfileImageService
    ) {
        this.attendanceRepository = attendanceRepository;
        this.breakRepository = breakRepository;
        this.employeeRepository = employeeRepository;
        this.locationVerificationService = locationVerificationService;
        this.faceService = faceService;
        this.openCvImageQualityService = openCvImageQualityService;
        this.currentCompanyService = currentCompanyService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.employeeProfileImageService = employeeProfileImageService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/face/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> verifyFace(
            Authentication authentication,
            @RequestPart("image") @NotNull MultipartFile image,
            @RequestPart(value = "descriptor", required = false) String descriptorJson) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Image is required"));
        }

        String qualityError = openCvImageQualityService.validate(image);
        if (qualityError != null) {
            return ResponseEntity.badRequest().body(Map.of("message", qualityError));
        }

        Company company = currentCompanyService.requireCompany(authentication);
        String username = currentCompanyService.requireUsername(authentication);

        Employee employee = employeeRepository.findByUserUsernameAndUserCompanyId(username, company.getId())
                .orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee profile not found"));
        }

        if (descriptorJson == null || descriptorJson.isBlank()) {
            boolean ok = faceService.verify(employee, image, null);
            if (ok) {
                try {
                    employeeProfileImageService.saveProfileImage(employee, image);
                    employeeRepository.save(employee);
                } catch (Exception ignored) {
                }
            }
            return ResponseEntity.ok(Map.of(
                    "faceVerified", ok,
                    "message", ok ? "Face verified" : "Face not verified"
            ));
        }

        AttendanceRecord attendance = attendanceRepository
                .findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(username, company.getId())
                .orElse(null);
        if (attendance == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active check-in found"));
        }

        boolean ok = faceService.verify(employee, image, descriptorJson);
        if (ok) {
            try {
                employeeProfileImageService.saveProfileImage(employee, image);
                employeeRepository.save(employee);
            } catch (Exception ignored) {
            }
        }
        attendance.setFaceVerified(ok);
        attendanceRepository.save(attendance);

        auditService.log(
                company.getId(),
                username,
                "FACE_VERIFY",
                "AttendanceRecord",
                attendance.getId(),
                "{\"faceVerified\":" + ok + "}"
        );

        return ResponseEntity.ok(Map.of(
                "faceVerified", ok,
                "message", ok ? "Face verified" : "Face not verified"
        ));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/break/start")
    public ResponseEntity<?> startBreak(Authentication authentication) {
        Company company = currentCompanyService.requireCompany(authentication);
        String username = currentCompanyService.requireUsername(authentication);

        AttendanceRecord attendance = attendanceRepository
                .findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(username, company.getId())
                .orElse(null);
        if (attendance == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active check-in found"));
        }

        boolean hasOpenBreak = breakRepository
                .findTopByAttendanceRecordIdAndBreakEndTimeIsNullOrderByBreakStartTimeDesc(attendance.getId())
                .isPresent();
        if (hasOpenBreak) {
            return ResponseEntity.badRequest().body(Map.of("message", "Break already started"));
        }

        BreakRecord record = BreakRecord.builder()
                .attendanceRecord(attendance)
                .breakStartTime(Instant.now())
                .build();

        BreakRecord saved = breakRepository.save(record);
        auditService.log(company.getId(), username, "BREAK_START", "AttendanceRecord", attendance.getId(), null);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/break/end")
    public ResponseEntity<?> endBreak(Authentication authentication) {
        Company company = currentCompanyService.requireCompany(authentication);
        String username = currentCompanyService.requireUsername(authentication);

        AttendanceRecord attendance = attendanceRepository
                .findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(username, company.getId())
                .orElse(null);
        if (attendance == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active check-in found"));
        }

        BreakRecord record = breakRepository
                .findTopByAttendanceRecordIdAndBreakEndTimeIsNullOrderByBreakStartTimeDesc(attendance.getId())
                .orElse(null);
        if (record == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active break found"));
        }

        record.setBreakEndTime(Instant.now());
        BreakRecord saved = breakRepository.save(record);
        auditService.log(company.getId(), username, "BREAK_END", "AttendanceRecord", attendance.getId(), null);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> checkIn(
            Authentication authentication,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "descriptor", required = false) String descriptorJson,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude
    ) {
        Company company = currentCompanyService.requireCompany(authentication);
        String username = currentCompanyService.requireUsername(authentication);

        Employee employee = employeeRepository.findByUserUsernameAndUserCompanyId(username, company.getId())
                .orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee profile not found"));
        }

        if (image != null && !image.isEmpty()) {
            String checkInQualityError = openCvImageQualityService.validate(image);
            if (checkInQualityError != null) {
                return ResponseEntity.badRequest().body(Map.of("message", checkInQualityError));
            }
        }

        if (!faceService.hasEnrollment(employee)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face not enrolled. Please enroll your face before checking in."));
        }
        if (descriptorJson == null || descriptorJson.isBlank()) {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Image or face descriptor is required"));
            }
        }

        boolean faceVerified = faceService.verify(employee, image, descriptorJson);
        if (!faceVerified) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face verification failed. The image does not match our records. Please try again with your enrolled photo."));
        }

        boolean alreadyCheckedIn = attendanceRepository
                .findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(username, company.getId())
                .isPresent();
        if (alreadyCheckedIn) {
            return ResponseEntity.badRequest().body(Map.of("message", "Already checked in"));
        }

        boolean locationVerified = locationVerificationService.isWithinAnyActiveLocation(
                company.getId(),
                latitude,
                longitude
        );

        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .checkInTime(Instant.now())
                .checkInLat(latitude)
                .checkInLng(longitude)
                .locationVerified(locationVerified)
                .faceVerified(faceVerified)
                .status(AttendanceStatus.PRESENT)
                .build();

        record = attendanceRepository.save(record);
        auditService.log(
                company.getId(),
                username,
                "CHECK_IN",
                "AttendanceRecord",
                record.getId(),
                "{\"lat\":" + latitude + ",\"lng\":" + longitude + ",\"locationVerified\":" + locationVerified + ",\"faceVerified\":" + faceVerified + "}"
        );
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasRole('RECORDER')")
    @PostMapping(value = "/recorder/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> recorderCheckIn(
            Authentication authentication,
            @RequestParam("employeeId") Long employeeId,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "descriptor", required = false) String descriptorJson,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude
    ) {
        Company company = currentCompanyService.requireCompany(authentication);
        String recorderUsername = currentCompanyService.requireUsername(authentication);

        if (employeeId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "employeeId is required"));
        }

        Employee employee = employeeRepository.findByIdAndUserCompanyId(employeeId, company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }

        if (image != null && !image.isEmpty()) {
            String checkInQualityError = openCvImageQualityService.validate(image);
            if (checkInQualityError != null) {
                return ResponseEntity.badRequest().body(Map.of("message", checkInQualityError));
            }
        }

        if (!faceService.hasEnrollment(employee)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face not enrolled. Please enroll face before checking in."));
        }
        if (descriptorJson == null || descriptorJson.isBlank()) {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Image or face descriptor is required"));
            }
        }

        boolean faceVerified = faceService.verify(employee, image, descriptorJson);
        if (!faceVerified) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face verification failed. The image does not match our records."));
        }

        boolean alreadyCheckedIn = attendanceRepository
                .findTopByEmployeeIdAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(employeeId, company.getId())
                .isPresent();
        if (alreadyCheckedIn) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee already checked in"));
        }

        boolean locationVerified = locationVerificationService.isWithinAnyActiveLocation(
                company.getId(),
                latitude,
                longitude
        );

        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .checkInTime(Instant.now())
                .checkInLat(latitude)
                .checkInLng(longitude)
                .locationVerified(locationVerified)
                .faceVerified(faceVerified)
                .status(AttendanceStatus.PRESENT)
                .build();

        record = attendanceRepository.save(record);
        auditService.log(
                company.getId(),
                recorderUsername,
                "RECORDER_CHECK_IN",
                "AttendanceRecord",
                record.getId(),
                "{\"employeeId\":" + employeeId + ",\"lat\":" + latitude + ",\"lng\":" + longitude + ",\"locationVerified\":" + locationVerified + ",\"faceVerified\":" + faceVerified + "}"
        );
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/check-out/company-purpose", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> checkOutCompanyPurpose(
            Authentication authentication,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "descriptor", required = false) String descriptorJson,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam("note") String note
    ) {
        Company company = currentCompanyService.requireCompany(authentication);
        String username = currentCompanyService.requireUsername(authentication);

        if (note == null || note.trim().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Note is required for company purpose clock-out"));
        }

        AttendanceRecord record = attendanceRepository
                .findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(username, company.getId())
                .orElse(null);

        if (record == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active check-in found"));
        }

        if (image != null && !image.isEmpty()) {
            String checkOutQualityError = openCvImageQualityService.validate(image);
            if (checkOutQualityError != null) {
                return ResponseEntity.badRequest().body(Map.of("message", checkOutQualityError));
            }
        }

        Employee employee = record.getEmployee();
        if (!faceService.hasEnrollment(employee)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face not enrolled. Please enroll your face before checking out."));
        }
        if (descriptorJson == null || descriptorJson.isBlank()) {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Image or face descriptor is required"));
            }
        }

        boolean faceVerified = faceService.verify(employee, image, descriptorJson);
        if (!faceVerified) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face verification failed. The image does not match our records. Please try again with your enrolled photo."));
        }

        record.setCheckOutTime(Instant.now());
        record.setCheckOutLat(latitude);
        record.setCheckOutLng(longitude);
        record.setFaceVerified(faceVerified);
        record.setClockOutType(ClockOutType.COMPANY_PURPOSE);
        record.setCompanyPurposeStatus(CompanyPurposeStatus.PENDING);
        record.setCompanyPurposeNote(note.trim());
        record.setCompanyPurposeApprovedAt(null);
        record.setCompanyPurposeApprovedBy(null);
        record.setCompanyPurposeDecisionNote(null);

        boolean locationVerified = locationVerificationService.isWithinAnyActiveLocation(
                company.getId(),
                latitude,
                longitude
        );
        record.setLocationVerified(record.isLocationVerified() && locationVerified);

        record = attendanceRepository.save(record);
        auditService.log(
                company.getId(),
                username,
                "CHECK_OUT_COMPANY_PURPOSE",
                "AttendanceRecord",
                record.getId(),
                "{\"lat\":" + latitude + ",\"lng\":" + longitude + ",\"locationVerified\":" + locationVerified + ",\"faceVerified\":" + faceVerified + ",\"status\":\"PENDING\"}"
        );

        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','PAYROLL','AUDITOR')")
    @GetMapping("/company-purpose/pending")
    public List<AttendanceResponse> listPendingCompanyPurpose(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant from = today.minusDays(31).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        return attendanceRepository.findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(from, to, company.getId())
                .stream()
                .filter(r -> (r.getClockOutType() == ClockOutType.COMPANY_PURPOSE) && (r.getCompanyPurposeStatus() == CompanyPurposeStatus.PENDING))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public static class CompanyPurposeDecisionRequest {
        private String note;

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','PAYROLL','AUDITOR')")
    @PostMapping("/{id}/company-purpose/approve")
    public ResponseEntity<?> approveCompanyPurpose(Authentication authentication, @PathVariable Long id, @RequestBody(required = false) CompanyPurposeDecisionRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        AttendanceRecord record = attendanceRepository.findByIdAndEmployeeUserCompanyId(id, company.getId()).orElse(null);
        if (record == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Attendance record not found"));
        }
        if (record.getClockOutType() != ClockOutType.COMPANY_PURPOSE || record.getCompanyPurposeStatus() != CompanyPurposeStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("message", "Attendance record is not pending company purpose"));
        }

        String approverUsername = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser approver = userRepository.findByUsernameAndCompanySlug(approverUsername, companySlug).orElse(null);
        if (approver == null) {
            return ResponseEntity.status(400).body(Map.of("message", "Approver user not found"));
        }

        record.setCompanyPurposeStatus(CompanyPurposeStatus.APPROVED);
        record.setCompanyPurposeApprovedBy(approver);
        record.setCompanyPurposeApprovedAt(Instant.now());
        record.setCompanyPurposeDecisionNote(request != null && request.getNote() != null && !request.getNote().trim().isBlank() ? request.getNote().trim() : null);
        record = attendanceRepository.save(record);

        auditService.log(company.getId(), approverUsername, "COMPANY_PURPOSE_APPROVE", "AttendanceRecord", record.getId(), null);
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','PAYROLL','AUDITOR')")
    @PostMapping("/{id}/company-purpose/reject")
    public ResponseEntity<?> rejectCompanyPurpose(Authentication authentication, @PathVariable Long id, @RequestBody(required = false) CompanyPurposeDecisionRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        AttendanceRecord record = attendanceRepository.findByIdAndEmployeeUserCompanyId(id, company.getId()).orElse(null);
        if (record == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Attendance record not found"));
        }
        if (record.getClockOutType() != ClockOutType.COMPANY_PURPOSE || record.getCompanyPurposeStatus() != CompanyPurposeStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("message", "Attendance record is not pending company purpose"));
        }

        String approverUsername = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser approver = userRepository.findByUsernameAndCompanySlug(approverUsername, companySlug).orElse(null);
        if (approver == null) {
            return ResponseEntity.status(400).body(Map.of("message", "Approver user not found"));
        }

        record.setCompanyPurposeStatus(CompanyPurposeStatus.REJECTED);
        record.setCompanyPurposeApprovedBy(approver);
        record.setCompanyPurposeApprovedAt(Instant.now());
        record.setCompanyPurposeDecisionNote(request != null && request.getNote() != null && !request.getNote().trim().isBlank() ? request.getNote().trim() : null);
        record = attendanceRepository.save(record);

        auditService.log(company.getId(), approverUsername, "COMPANY_PURPOSE_REJECT", "AttendanceRecord", record.getId(), null);
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/check-out", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> checkOut(
            Authentication authentication,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "descriptor", required = false) String descriptorJson,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude
    ) {
        Company company = currentCompanyService.requireCompany(authentication);
        String username = currentCompanyService.requireUsername(authentication);

        AttendanceRecord record = attendanceRepository
                .findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(username, company.getId())
                .orElse(null);

        if (record == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active check-in found"));
        }

        if (image != null && !image.isEmpty()) {
            String checkOutQualityError = openCvImageQualityService.validate(image);
            if (checkOutQualityError != null) {
                return ResponseEntity.badRequest().body(Map.of("message", checkOutQualityError));
            }
        }

        Employee employee = record.getEmployee();
        if (!faceService.hasEnrollment(employee)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face not enrolled. Please enroll your face before checking out."));
        }
        if (descriptorJson == null || descriptorJson.isBlank()) {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Image or face descriptor is required"));
            }
        }

        boolean faceVerified = faceService.verify(employee, image, descriptorJson);
        if (!faceVerified) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face verification failed. The image does not match our records. Please try again with your enrolled photo."));
        }

        record.setCheckOutTime(Instant.now());
        record.setCheckOutLat(latitude);
        record.setCheckOutLng(longitude);
        record.setFaceVerified(faceVerified);
        record.setClockOutType(ClockOutType.NORMAL);
        record.setCompanyPurposeStatus(CompanyPurposeStatus.NONE);
        record.setCompanyPurposeNote(null);
        record.setCompanyPurposeApprovedAt(null);
        record.setCompanyPurposeApprovedBy(null);
        record.setCompanyPurposeDecisionNote(null);

        boolean locationVerified = locationVerificationService.isWithinAnyActiveLocation(
                company.getId(),
                latitude,
                longitude
        );
        record.setLocationVerified(record.isLocationVerified() && locationVerified);

        record = attendanceRepository.save(record);
        auditService.log(
                company.getId(),
                username,
                "CHECK_OUT",
                "AttendanceRecord",
                record.getId(),
                "{\"lat\":" + latitude + ",\"lng\":" + longitude + ",\"locationVerified\":" + locationVerified + ",\"faceVerified\":" + faceVerified + "}"
        );
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasRole('RECORDER')")
    @PostMapping(value = "/recorder/check-out", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> recorderCheckOut(
            Authentication authentication,
            @RequestParam("employeeId") Long employeeId,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "descriptor", required = false) String descriptorJson,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude
    ) {
        Company company = currentCompanyService.requireCompany(authentication);
        String recorderUsername = currentCompanyService.requireUsername(authentication);

        if (employeeId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "employeeId is required"));
        }

        AttendanceRecord record = attendanceRepository
                .findTopByEmployeeIdAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(employeeId, company.getId())
                .orElse(null);
        if (record == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active check-in found"));
        }

        if (image != null && !image.isEmpty()) {
            String checkOutQualityError = openCvImageQualityService.validate(image);
            if (checkOutQualityError != null) {
                return ResponseEntity.badRequest().body(Map.of("message", checkOutQualityError));
            }
        }

        Employee employee = record.getEmployee();
        if (!faceService.hasEnrollment(employee)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face not enrolled. Please enroll face before checking out."));
        }
        if (descriptorJson == null || descriptorJson.isBlank()) {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Image or face descriptor is required"));
            }
        }

        boolean faceVerified = faceService.verify(employee, image, descriptorJson);
        if (!faceVerified) {
            return ResponseEntity.badRequest().body(Map.of("message", "Face verification failed. The image does not match our records."));
        }

        record.setCheckOutTime(Instant.now());
        record.setCheckOutLat(latitude);
        record.setCheckOutLng(longitude);
        record.setFaceVerified(faceVerified);
        record.setClockOutType(ClockOutType.NORMAL);
        record.setCompanyPurposeStatus(CompanyPurposeStatus.NONE);
        record.setCompanyPurposeNote(null);
        record.setCompanyPurposeApprovedAt(null);
        record.setCompanyPurposeApprovedBy(null);
        record.setCompanyPurposeDecisionNote(null);

        boolean locationVerified = locationVerificationService.isWithinAnyActiveLocation(
                company.getId(),
                latitude,
                longitude
        );
        record.setLocationVerified(record.isLocationVerified() && locationVerified);

        record = attendanceRepository.save(record);
        auditService.log(
                company.getId(),
                recorderUsername,
                "RECORDER_CHECK_OUT",
                "AttendanceRecord",
                record.getId(),
                "{\"employeeId\":" + employeeId + ",\"lat\":" + latitude + ",\"lng\":" + longitude + ",\"locationVerified\":" + locationVerified + ",\"faceVerified\":" + faceVerified + "}"
        );
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public List<AttendanceResponse> my(Authentication authentication) {
        Company company = currentCompanyService.requireCompany(authentication);
        return attendanceRepository.findByEmployeeUserUsernameAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(currentCompanyService.requireUsername(authentication), company.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER')")
    @GetMapping
    public List<AttendanceResponse> listToday(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant from = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return attendanceRepository.findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(from, to, company.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        AttendanceRecord record = attendanceRepository.findByIdAndEmployeeUserCompanyId(id, company.getId()).orElse(null);
        if (record == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Attendance record not found"));
        }
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> listByEmployee(Authentication authentication, @PathVariable Long employeeId, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Employee employee = employeeRepository.findByIdAndUserCompanyId(employeeId, company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }

        String username = employee.getUser() != null ? employee.getUser().getUsername() : null;
        if (username == null) {
            return ResponseEntity.status(400).body(Map.of("message", "Employee user not found"));
        }

        return ResponseEntity.ok(
                attendanceRepository.findByEmployeeUserUsernameAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(username, company.getId())
                        .stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList())
        );
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PostMapping("/admin")
    public ResponseEntity<?> createForEmployee(Authentication authentication, @Valid @RequestBody AdminUpsertAttendanceRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        if (request.getEmployeeId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "employeeId is required"));
        }

        Employee employee = employeeRepository.findByIdAndUserCompanyId(request.getEmployeeId(), company.getId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .checkInTime(request.getCheckInTime())
                .checkOutTime(request.getCheckOutTime())
                .checkInLat(request.getCheckInLat())
                .checkInLng(request.getCheckInLng())
                .checkOutLat(request.getCheckOutLat())
                .checkOutLng(request.getCheckOutLng())
                .locationVerified(request.getLocationVerified() != null ? request.getLocationVerified() : false)
                .faceVerified(request.getFaceVerified() != null ? request.getFaceVerified() : false)
                .status(request.getStatus() != null ? request.getStatus() : AttendanceStatus.PRESENT)
                .build();

        record = attendanceRepository.save(record);
        auditService.log(company.getId(), currentCompanyService.requireUsername(authentication), "ATTENDANCE_CREATE", "AttendanceRecord", record.getId(), null);
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PostMapping("/admin/bulk")
    public ResponseEntity<?> bulkCreateForEmployees(Authentication authentication,
                                                    @Valid @RequestBody BulkTimesheetImportRequest request,
                                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        List<BulkTimesheetImportResponse.RowResult> results = new ArrayList<>();
        int ok = 0;
        int failed = 0;

        List<BulkTimesheetImportRequest.Row> rows = request.getRows() != null ? request.getRows() : List.of();
        for (int i = 0; i < rows.size(); i += 1) {
            BulkTimesheetImportRequest.Row row = rows.get(i);
            Long employeeId = row.getEmployeeId();
            try {
                Employee employee = employeeRepository.findByIdAndUserCompanyId(employeeId, company.getId()).orElse(null);
                if (employee == null) {
                    failed += 1;
                    results.add(new BulkTimesheetImportResponse.RowResult(i, employeeId, false, "Employee not found", null));
                    continue;
                }
                if (row.getCheckInTime() == null || row.getCheckOutTime() == null) {
                    failed += 1;
                    results.add(new BulkTimesheetImportResponse.RowResult(i, employeeId, false, "checkInTime and checkOutTime are required", null));
                    continue;
                }
                if (!row.getCheckOutTime().isAfter(row.getCheckInTime())) {
                    failed += 1;
                    results.add(new BulkTimesheetImportResponse.RowResult(i, employeeId, false, "checkOutTime must be after checkInTime", null));
                    continue;
                }

                AttendanceRecord record = AttendanceRecord.builder()
                        .employee(employee)
                        .checkInTime(row.getCheckInTime())
                        .checkOutTime(row.getCheckOutTime())
                        .locationVerified(row.getLocationVerified() != null ? row.getLocationVerified() : false)
                        .faceVerified(row.getFaceVerified() != null ? row.getFaceVerified() : false)
                        .status(AttendanceStatus.PRESENT)
                        .build();

                record = attendanceRepository.save(record);
                ok += 1;
                results.add(new BulkTimesheetImportResponse.RowResult(i, employeeId, true, "OK", record.getId()));
            } catch (Exception e) {
                failed += 1;
                results.add(new BulkTimesheetImportResponse.RowResult(i, employeeId, false, e.getMessage() != null ? e.getMessage() : "Failed", null));
            }
        }

        auditService.log(
                company.getId(),
                currentCompanyService.requireUsername(authentication),
                "TIMESHEET_IMPORT",
                "AttendanceRecord",
                null,
                "{\"ok\":" + ok + ",\"failed\":" + failed + ",\"total\":" + rows.size() + "}"
        );

        return ResponseEntity.ok(new BulkTimesheetImportResponse(ok, failed, results));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PatchMapping("/admin/{id}")
    public ResponseEntity<?> updateForEmployee(Authentication authentication, @PathVariable Long id, @RequestBody AdminUpsertAttendanceRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        AttendanceRecord record = attendanceRepository.findByIdAndEmployeeUserCompanyId(id, company.getId()).orElse(null);
        if (record == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Attendance record not found"));
        }

        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdAndUserCompanyId(request.getEmployeeId(), company.getId()).orElse(null);
            if (employee == null) {
                return ResponseEntity.status(404).body(Map.of("message", "Employee not found"));
            }
            record.setEmployee(employee);
        }

        if (request.getCheckInTime() != null) {
            record.setCheckInTime(request.getCheckInTime());
        }
        if (request.getCheckOutTime() != null) {
            record.setCheckOutTime(request.getCheckOutTime());
        }
        if (request.getCheckInLat() != null) {
            record.setCheckInLat(request.getCheckInLat());
        }
        if (request.getCheckInLng() != null) {
            record.setCheckInLng(request.getCheckInLng());
        }
        if (request.getCheckOutLat() != null) {
            record.setCheckOutLat(request.getCheckOutLat());
        }
        if (request.getCheckOutLng() != null) {
            record.setCheckOutLng(request.getCheckOutLng());
        }
        if (request.getLocationVerified() != null) {
            record.setLocationVerified(request.getLocationVerified());
        }
        if (request.getFaceVerified() != null) {
            record.setFaceVerified(request.getFaceVerified());
        }
        if (request.getStatus() != null) {
            record.setStatus(request.getStatus());
        }

        record = attendanceRepository.save(record);
        auditService.log(company.getId(), currentCompanyService.requireUsername(authentication), "ATTENDANCE_UPDATE", "AttendanceRecord", record.getId(), null);
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteForEmployee(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        AttendanceRecord record = attendanceRepository.findByIdAndEmployeeUserCompanyId(id, company.getId()).orElse(null);
        if (record == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Attendance record not found"));
        }

        attendanceRepository.delete(record);
        auditService.log(company.getId(), currentCompanyService.requireUsername(authentication), "ATTENDANCE_DELETE", "AttendanceRecord", id, null);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    public static class AdminUpsertAttendanceRequest {
        private Long employeeId;
        private Instant checkInTime;
        private Instant checkOutTime;
        private Double checkInLat;
        private Double checkInLng;
        private Double checkOutLat;
        private Double checkOutLng;
        private Boolean locationVerified;
        private Boolean faceVerified;
        private AttendanceStatus status;

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public Instant getCheckInTime() {
            return checkInTime;
        }

        public void setCheckInTime(Instant checkInTime) {
            this.checkInTime = checkInTime;
        }

        public Instant getCheckOutTime() {
            return checkOutTime;
        }

        public void setCheckOutTime(Instant checkOutTime) {
            this.checkOutTime = checkOutTime;
        }

        public Double getCheckInLat() {
            return checkInLat;
        }

        public void setCheckInLat(Double checkInLat) {
            this.checkInLat = checkInLat;
        }

        public Double getCheckInLng() {
            return checkInLng;
        }

        public void setCheckInLng(Double checkInLng) {
            this.checkInLng = checkInLng;
        }

        public Double getCheckOutLat() {
            return checkOutLat;
        }

        public void setCheckOutLat(Double checkOutLat) {
            this.checkOutLat = checkOutLat;
        }

        public Double getCheckOutLng() {
            return checkOutLng;
        }

        public void setCheckOutLng(Double checkOutLng) {
            this.checkOutLng = checkOutLng;
        }

        public Boolean getLocationVerified() {
            return locationVerified;
        }

        public void setLocationVerified(Boolean locationVerified) {
            this.locationVerified = locationVerified;
        }

        public Boolean getFaceVerified() {
            return faceVerified;
        }

        public void setFaceVerified(Boolean faceVerified) {
            this.faceVerified = faceVerified;
        }

        public AttendanceStatus getStatus() {
            return status;
        }

        public void setStatus(AttendanceStatus status) {
            this.status = status;
        }
    }

    private AttendanceResponse toResponse(AttendanceRecord record) {
        Employee employee = record.getEmployee();

        long breakMinutes = calculateBreakMinutes(record);
        long workedMinutes = calculateWorkedMinutes(record, breakMinutes);

        return new AttendanceResponse(
                record.getId(),
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                record.getCheckInTime(),
                record.getCheckOutTime(),
                record.getCheckInLat(),
                record.getCheckInLng(),
                record.getCheckOutLat(),
                record.getCheckOutLng(),
                record.isLocationVerified(),
                record.isFaceVerified(),
                record.getStatus(),
                record.getClockOutType() != null ? record.getClockOutType() : ClockOutType.NORMAL,
                record.getCompanyPurposeStatus() != null ? record.getCompanyPurposeStatus() : CompanyPurposeStatus.NONE,
                record.getCompanyPurposeNote(),
                workedMinutes,
                breakMinutes
        );
    }

    /**
     * Calculate total break minutes for a given attendance record.
     * Only closed breaks (with an end time) are counted.
     */
    private long calculateBreakMinutes(AttendanceRecord record) {
        if (record.getId() == null) {
            return 0;
        }
        return breakRepository.findByAttendanceRecordIdOrderByBreakStartTimeAsc(record.getId())
                .stream()
                .filter(b -> b.getBreakEndTime() != null)
                .mapToLong(b -> {
                    Duration d = Duration.between(b.getBreakStartTime(), b.getBreakEndTime());
                    return Math.max(0, d.toMinutes());
                })
                .sum();
    }

    /**
     * Calculate net worked minutes for a record: total duration minus breaks.
     */
    private long calculateWorkedMinutes(AttendanceRecord record, long breakMinutes) {
        if (record.getCheckInTime() == null || record.getCheckOutTime() == null) {
            return 0;
        }

        ClockOutType type = record.getClockOutType() != null ? record.getClockOutType() : ClockOutType.NORMAL;
        CompanyPurposeStatus cp = record.getCompanyPurposeStatus() != null ? record.getCompanyPurposeStatus() : CompanyPurposeStatus.NONE;
        if (type == ClockOutType.COMPANY_PURPOSE && cp != CompanyPurposeStatus.APPROVED) {
            return 0;
        }

        Duration total = Duration.between(record.getCheckInTime(), record.getCheckOutTime());
        long totalMinutes = Math.max(0, total.toMinutes());
        long net = totalMinutes - breakMinutes;
        return Math.max(0, net);
    }
}
