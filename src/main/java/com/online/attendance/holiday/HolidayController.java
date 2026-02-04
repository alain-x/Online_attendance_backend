package com.online.attendance.holiday;

import com.online.attendance.company.Company;
import com.online.attendance.holiday.dto.CreateHolidayRequest;
import com.online.attendance.holiday.dto.UpdateHolidayRequest;
import com.online.attendance.security.CurrentCompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayRepository holidayRepository;
    private final CurrentCompanyService currentCompanyService;

    public HolidayController(HolidayRepository holidayRepository, CurrentCompanyService currentCompanyService) {
        this.holidayRepository = holidayRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER')")
    @GetMapping
    public List<Holiday> list(Authentication authentication,
                              @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                              @RequestParam(required = false) String from,
                              @RequestParam(required = false) String to) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            LocalDate f = LocalDate.parse(from);
            LocalDate t = LocalDate.parse(to);
            return holidayRepository.findByCompanyIdAndDateBetweenOrderByDateAsc(company.getId(), f, t);
        }

        return holidayRepository.findByCompanyIdOrderByDateAsc(company.getId());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication,
                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                    @Valid @RequestBody CreateHolidayRequest request) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        LocalDate date = request.getDate();
        if (holidayRepository.existsByCompanyIdAndDate(company.getId(), date)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Holiday already exists for this date"));
        }

        Holiday h = Holiday.builder()
                .company(company)
                .date(date)
                .name(request.getName().trim())
                .build();

        return ResponseEntity.ok(holidayRepository.save(h));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication authentication,
                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                    @PathVariable Long id,
                                    @Valid @RequestBody UpdateHolidayRequest request) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        Holiday h = holidayRepository.findByIdAndCompanyId(id, company.getId());
        if (h == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Holiday not found"));
        }

        LocalDate date = request.getDate();
        if (holidayRepository.existsByCompanyIdAndDateAndIdNot(company.getId(), date, id)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Another holiday already exists for this date"));
        }

        h.setDate(date);
        h.setName(request.getName().trim());
        return ResponseEntity.ok(holidayRepository.save(h));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication authentication,
                                    @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                    @PathVariable Long id) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        Holiday h = holidayRepository.findByIdAndCompanyId(id, company.getId());
        if (h == null) {
            return ResponseEntity.noContent().build();
        }

        holidayRepository.delete(h);
        return ResponseEntity.noContent().build();
    }
}
