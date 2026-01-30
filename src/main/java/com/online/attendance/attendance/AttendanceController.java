package com.online.attendance.attendance;

import com.online.attendance.audit.AuditService;
import com.online.attendance.attendance.dto.AttendanceResponse;
import com.online.attendance.attendance.dto.CheckInRequest;
import com.online.attendance.attendance.dto.CheckOutRequest;
import com.online.attendance.company.Company;
import com.online.attendance.employee.Employee;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.face.FaceService;
import com.online.attendance.location.LocationVerificationService;
import com.online.attendance.security.CurrentCompanyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final BreakRepository breakRepository;
    private final EmployeeRepository employeeRepository;
    private final LocationVerificationService locationVerificationService;
    private final FaceService faceService;
    private final CurrentCompanyService currentCompanyService;
    private final AuditService auditService;

    public AttendanceController(
            AttendanceRepository attendanceRepository,
            BreakRepository breakRepository,
            EmployeeRepository employeeRepository,
            LocationVerificationService locationVerificationService,
            FaceService faceService,
            CurrentCompanyService currentCompanyService,
            AuditService auditService
    ) {
        this.attendanceRepository = attendanceRepository;
        this.breakRepository = breakRepository;
        this.employeeRepository = employeeRepository;
        this.locationVerificationService = locationVerificationService;
        this.faceService = faceService;
        this.currentCompanyService = currentCompanyService;
        this.auditService = auditService;
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping(value = "/face/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> verifyFace(Authentication authentication, @RequestPart("image") @NotNull MultipartFile image) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Image is required"));
        }

        Company company = currentCompanyService.requireCompany(authentication);
        String username = currentCompanyService.requireUsername(authentication);

        Employee employee = employeeRepository.findByUserUsernameAndUserCompanyId(username, company.getId())
                .orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee profile not found"));
        }

        AttendanceRecord attendance = attendanceRepository
                .findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(username, company.getId())
                .orElse(null);
        if (attendance == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active check-in found"));
        }

        boolean ok = faceService.verify(employee, image);
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

    @PreAuthorize("hasRole('EMPLOYEE')")
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

    @PreAuthorize("hasRole('EMPLOYEE')")
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

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(Authentication authentication, @Valid @RequestBody CheckInRequest request) {
        Company company = currentCompanyService.requireCompany(authentication);
        String username = currentCompanyService.requireUsername(authentication);

        Employee employee = employeeRepository.findByUserUsernameAndUserCompanyId(username, company.getId())
                .orElse(null);
        if (employee == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee profile not found"));
        }

        boolean alreadyCheckedIn = attendanceRepository
                .findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(username, company.getId())
                .isPresent();
        if (alreadyCheckedIn) {
            return ResponseEntity.badRequest().body(Map.of("message", "Already checked in"));
        }

        boolean locationVerified = locationVerificationService.isWithinAnyActiveLocation(
                company.getId(),
                request.getLatitude(),
                request.getLongitude()
        );

        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .checkInTime(Instant.now())
                .checkInLat(request.getLatitude())
                .checkInLng(request.getLongitude())
                .locationVerified(locationVerified)
                .faceVerified(false)
                .status(AttendanceStatus.PRESENT)
                .build();

        record = attendanceRepository.save(record);
        auditService.log(
                company.getId(),
                username,
                "CHECK_IN",
                "AttendanceRecord",
                record.getId(),
                "{\"lat\":" + request.getLatitude() + ",\"lng\":" + request.getLongitude() + ",\"locationVerified\":" + locationVerified + "}"
        );
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping("/check-out")
    public ResponseEntity<?> checkOut(Authentication authentication, @Valid @RequestBody CheckOutRequest request) {
        Company company = currentCompanyService.requireCompany(authentication);
        String username = currentCompanyService.requireUsername(authentication);

        AttendanceRecord record = attendanceRepository
                .findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(username, company.getId())
                .orElse(null);

        if (record == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active check-in found"));
        }

        record.setCheckOutTime(Instant.now());
        record.setCheckOutLat(request.getLatitude());
        record.setCheckOutLng(request.getLongitude());

        boolean locationVerified = locationVerificationService.isWithinAnyActiveLocation(
                company.getId(),
                request.getLatitude(),
                request.getLongitude()
        );
        record.setLocationVerified(record.isLocationVerified() && locationVerified);

        record = attendanceRepository.save(record);
        auditService.log(
                company.getId(),
                username,
                "CHECK_OUT",
                "AttendanceRecord",
                record.getId(),
                "{\"lat\":" + request.getLatitude() + ",\"lng\":" + request.getLongitude() + ",\"locationVerified\":" + locationVerified + "}"
        );
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/my")
    public List<AttendanceResponse> my(Authentication authentication) {
        Company company = currentCompanyService.requireCompany(authentication);
        return attendanceRepository.findByEmployeeUserUsernameAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(currentCompanyService.requireUsername(authentication), company.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @GetMapping
    public List<AttendanceResponse> listToday(Authentication authentication) {
        Company company = currentCompanyService.requireCompany(authentication);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant from = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return attendanceRepository.findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(from, to, company.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(Authentication authentication, @PathVariable Long id) {
        Company company = currentCompanyService.requireCompany(authentication);
        AttendanceRecord record = attendanceRepository.findByIdAndEmployeeUserCompanyId(id, company.getId()).orElse(null);
        if (record == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Attendance record not found"));
        }
        return ResponseEntity.ok(toResponse(record));
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> listByEmployee(Authentication authentication, @PathVariable Long employeeId) {
        Company company = currentCompanyService.requireCompany(authentication);
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

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PostMapping("/admin")
    public ResponseEntity<?> createForEmployee(Authentication authentication, @Valid @RequestBody AdminUpsertAttendanceRequest request) {
        Company company = currentCompanyService.requireCompany(authentication);

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

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PatchMapping("/admin/{id}")
    public ResponseEntity<?> updateForEmployee(Authentication authentication, @PathVariable Long id, @RequestBody AdminUpsertAttendanceRequest request) {
        Company company = currentCompanyService.requireCompany(authentication);
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

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteForEmployee(Authentication authentication, @PathVariable Long id) {
        Company company = currentCompanyService.requireCompany(authentication);
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
        Duration total = Duration.between(record.getCheckInTime(), record.getCheckOutTime());
        long totalMinutes = Math.max(0, total.toMinutes());
        long net = totalMinutes - breakMinutes;
        return Math.max(0, net);
    }
}
