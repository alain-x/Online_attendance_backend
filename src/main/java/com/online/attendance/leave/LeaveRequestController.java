package com.online.attendance.leave;

import com.online.attendance.company.Company;
import com.online.attendance.employee.Employee;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.leave.dto.CreateLeaveRequest;
import com.online.attendance.leave.dto.DecisionLeaveRequest;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.Role;
import com.online.attendance.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave/requests")
public class LeaveRequestController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final CurrentCompanyService currentCompanyService;

    public LeaveRequestController(
            LeaveRequestRepository leaveRequestRepository,
            LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            CurrentCompanyService currentCompanyService
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','EMPLOYEE')")
    @GetMapping("/my")
    public ResponseEntity<?> my(Authentication authentication,
                                @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);

        AppUser user = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (user == null || user.getId() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        Employee employee = employeeRepository.findByUserIdAndUserCompanyId(user.getId(), company.getId()).orElse(null);
        if (employee == null || employee.getId() == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(leaveRequestRepository.findByEmployeeIdAndCompanyIdOrderByCreatedAtDesc(employee.getId(), company.getId()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER')")
    @GetMapping
    public List<LeaveRequest> list(Authentication authentication,
                                   @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            LocalDate f = LocalDate.parse(from);
            LocalDate t = LocalDate.parse(to);
            if (status != null && !status.isBlank()) {
                LeaveRequestStatus st = LeaveRequestStatus.valueOf(status.trim().toUpperCase());
                return leaveRequestRepository.findByCompanyIdAndFromDateLessThanEqualAndToDateGreaterThanEqualAndStatus(company.getId(), t, f, st);
            }
            return leaveRequestRepository.findByCompanyIdAndFromDateLessThanEqualAndToDateGreaterThanEqualAndStatus(company.getId(), t, f, LeaveRequestStatus.APPROVED);
        }

        if (status != null && !status.isBlank()) {
            LeaveRequestStatus st = LeaveRequestStatus.valueOf(status.trim().toUpperCase());
            return leaveRequestRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(company.getId(), st);
        }

        return leaveRequestRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','EMPLOYEE')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication,
                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                    @Valid @RequestBody CreateLeaveRequest request) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser currentUser = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (currentUser == null || currentUser.getRole() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        Employee employee;
        if (currentUser.getRole() == Role.EMPLOYEE) {
            employee = employeeRepository.findByUserIdAndUserCompanyId(currentUser.getId(), company.getId()).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Employee profile not found"));
            }
        } else {
            if (request.getEmployeeId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "employeeId is required"));
            }
            employee = employeeRepository.findByIdAndUserCompanyId(request.getEmployeeId(), company.getId()).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Employee not found"));
            }
        }

        LeaveType type = leaveTypeRepository.findByIdAndCompanyId(request.getLeaveTypeId(), company.getId());
        if (type == null || !type.isActive()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Leave type not found"));
        }

        if (request.getFromDate().isAfter(request.getToDate())) {
            return ResponseEntity.badRequest().body(Map.of("message", "fromDate must be <= toDate"));
        }

        if (request.getUnit() == LeaveUnit.HALF_DAY && request.getHalfDayPart() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "halfDayPart is required for HALF_DAY"));
        }

        if (request.getUnit() == LeaveUnit.HOURS) {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "startTime and endTime are required for HOURS"));
            }
            if (!request.getFromDate().equals(request.getToDate())) {
                return ResponseEntity.badRequest().body(Map.of("message", "HOURS leave must be within a single day"));
            }
        }

        if (request.getUnit() == LeaveUnit.HALF_DAY && !request.getFromDate().equals(request.getToDate())) {
            return ResponseEntity.badRequest().body(Map.of("message", "HALF_DAY leave must be within a single day"));
        }

        LeaveRequest lr = LeaveRequest.builder()
                .company(company)
                .employee(employee)
                .leaveType(type)
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .unit(request.getUnit())
                .halfDayPart(request.getHalfDayPart())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason() != null ? request.getReason().trim() : null)
                .status(LeaveRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        return ResponseEntity.ok(leaveRequestRepository.save(lr));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER')")
    @PutMapping("/{id}/decision")
    public ResponseEntity<?> decide(Authentication authentication,
                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                    @PathVariable Long id,
                                    @Valid @RequestBody DecisionLeaveRequest request) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        LeaveRequest lr = leaveRequestRepository.findByIdAndCompanyId(id, company.getId());
        if (lr == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Leave request not found"));
        }

        if (lr.getStatus() != LeaveRequestStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only PENDING requests can be decided"));
        }

        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser currentUser = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        lr.setStatus(Boolean.TRUE.equals(request.getApprove()) ? LeaveRequestStatus.APPROVED : LeaveRequestStatus.REJECTED);
        lr.setDecidedAt(Instant.now());
        lr.setDecidedBy(currentUser);
        lr.setDecisionNote(request.getNote() != null ? request.getNote().trim() : null);

        return ResponseEntity.ok(leaveRequestRepository.save(lr));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','EMPLOYEE')")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(Authentication authentication,
                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                    @PathVariable Long id) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        LeaveRequest lr = leaveRequestRepository.findByIdAndCompanyId(id, company.getId());
        if (lr == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Leave request not found"));
        }

        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser currentUser = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (currentUser == null || currentUser.getRole() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        if (lr.getStatus() != LeaveRequestStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only PENDING requests can be cancelled"));
        }

        if (currentUser.getRole() == Role.EMPLOYEE) {
            Employee self = employeeRepository.findByUserIdAndUserCompanyId(currentUser.getId(), company.getId()).orElse(null);
            if (self == null || self.getId() == null || lr.getEmployee() == null || !self.getId().equals(lr.getEmployee().getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Employees can only cancel their own requests"));
            }
        }

        lr.setStatus(LeaveRequestStatus.CANCELLED);
        return ResponseEntity.ok(leaveRequestRepository.save(lr));
    }
}
