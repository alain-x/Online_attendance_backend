package com.online.attendance.leave;

import com.online.attendance.company.Company;
import com.online.attendance.leave.dto.CreateLeaveTypeRequest;
import com.online.attendance.leave.dto.UpdateLeaveTypeRequest;
import com.online.attendance.security.CurrentCompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave/types")
public class LeaveTypeController {

    private final LeaveTypeRepository leaveTypeRepository;
    private final CurrentCompanyService currentCompanyService;

    public LeaveTypeController(LeaveTypeRepository leaveTypeRepository, CurrentCompanyService currentCompanyService) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER')")
    @GetMapping
    public List<LeaveType> list(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        List<LeaveType> existing = leaveTypeRepository.findByCompanyIdOrderByNameAsc(company.getId());
        if (!existing.isEmpty()) {
            return existing;
        }

        LeaveType[] defaults = new LeaveType[] {
                LeaveType.builder().company(company).code("ANNUAL").name("Annual Leave").paid(true).active(true).build(),
                LeaveType.builder().company(company).code("SICK").name("Sick Leave").paid(true).active(true).build(),
                LeaveType.builder().company(company).code("UNPAID").name("Unpaid Leave").paid(false).active(true).build(),
                LeaveType.builder().company(company).code("MATERNITY").name("Maternity Leave").paid(true).active(true).build(),
                LeaveType.builder().company(company).code("PATERNITY").name("Paternity Leave").paid(true).active(true).build(),
                LeaveType.builder().company(company).code("COMPASSIONATE").name("Compassionate Leave").paid(true).active(true).build(),
                LeaveType.builder().company(company).code("STUDY").name("Study Leave").paid(true).active(true).build(),
                LeaveType.builder().company(company).code("OTHER").name("Other").paid(false).active(true).build()
        };
        for (LeaveType t : defaults) {
            if (!leaveTypeRepository.existsByCompanyIdAndCode(company.getId(), t.getCode())) {
                leaveTypeRepository.save(t);
            }
        }
        return leaveTypeRepository.findByCompanyIdOrderByNameAsc(company.getId());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication,
                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                    @Valid @RequestBody CreateLeaveTypeRequest request) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        String code = request.getCode().trim().toUpperCase();
        if (leaveTypeRepository.existsByCompanyIdAndCode(company.getId(), code)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Leave type code already exists"));
        }

        LeaveType t = LeaveType.builder()
                .company(company)
                .code(code)
                .name(request.getName().trim())
                .paid(request.isPaid())
                .active(request.getActive() == null || request.getActive())
                .build();

        return ResponseEntity.ok(leaveTypeRepository.save(t));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication authentication,
                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                    @PathVariable Long id,
                                    @Valid @RequestBody UpdateLeaveTypeRequest request) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        LeaveType t = leaveTypeRepository.findByIdAndCompanyId(id, company.getId());
        if (t == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Leave type not found"));
        }

        String code = request.getCode().trim().toUpperCase();
        if (leaveTypeRepository.existsByCompanyIdAndCodeAndIdNot(company.getId(), code, id)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Another leave type already uses this code"));
        }

        t.setCode(code);
        t.setName(request.getName().trim());
        t.setPaid(request.isPaid());
        if (request.getActive() != null) {
            t.setActive(request.getActive());
        }

        return ResponseEntity.ok(leaveTypeRepository.save(t));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication authentication,
                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                    @PathVariable Long id) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        LeaveType t = leaveTypeRepository.findByIdAndCompanyId(id, company.getId());
        if (t == null) {
            return ResponseEntity.noContent().build();
        }

        leaveTypeRepository.delete(t);
        return ResponseEntity.noContent().build();
    }
}
