package com.online.attendance.location;

import com.online.attendance.company.Company;
import com.online.attendance.location.dto.CreateWorkLocationRequest;
import com.online.attendance.location.dto.UpdateWorkLocationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.online.attendance.security.CurrentCompanyService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
public class WorkLocationController {

    private final WorkLocationRepository workLocationRepository;
    private final CurrentCompanyService currentCompanyService;

    public WorkLocationController(WorkLocationRepository workLocationRepository, CurrentCompanyService currentCompanyService) {
        this.workLocationRepository = workLocationRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody CreateWorkLocationRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        WorkLocation location = WorkLocation.builder()
                .name(request.getName())
                .company(company)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .radiusMeters(request.getRadiusMeters())
                .active(request.getActive())
                .build();

        location = workLocationRepository.save(location);
        return ResponseEntity.ok(location);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER')")
    @GetMapping
    public List<WorkLocation> list(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        return workLocationRepository.findByCompanyId(company.getId());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        WorkLocation location = workLocationRepository.findByIdAndCompanyId(id, company.getId());
        if (location == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Location not found"));
        }
        return ResponseEntity.ok(location);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','EMPLOYEE')")
    @GetMapping("/active")
    public List<WorkLocation> listActive(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        return workLocationRepository.findByActiveTrueAndCompanyId(company.getId());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PutMapping("/{id}/active")
    public ResponseEntity<?> setActive(Authentication authentication, @PathVariable Long id, @RequestParam boolean active, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        WorkLocation location = workLocationRepository.findByIdAndCompanyId(id, company.getId());
        if (location == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Location not found"));
        }
        location.setActive(active);
        return ResponseEntity.ok(workLocationRepository.save(location));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody UpdateWorkLocationRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        WorkLocation location = workLocationRepository.findByIdAndCompanyId(id, company.getId());
        if (location == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Location not found"));
        }

        location.setName(request.getName());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setRadiusMeters(request.getRadiusMeters());
        location.setActive(request.getActive());

        return ResponseEntity.ok(workLocationRepository.save(location));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        WorkLocation location = workLocationRepository.findByIdAndCompanyId(id, company.getId());
        if (location == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Location not found"));
        }

        workLocationRepository.delete(location);
        return ResponseEntity.noContent().build();
    }
}
