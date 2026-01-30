package com.online.attendance.odoo;

import com.online.attendance.attendance.AttendanceRecord;
import com.online.attendance.attendance.AttendanceRepository;
import com.online.attendance.company.Company;
import com.online.attendance.security.CurrentCompanyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Odoo sync scaffold.
 *
 * Professional design: keep sync logic behind a service so we can later replace this
 * with real Odoo XML-RPC/JSON-RPC calls + mapping to Odoo models (hr.attendance).
 *
 * Current implementation is a SAFE NO-OP when odoo.enabled=false.
 */
@Service
public class OdooSyncService {

    private final OdooConfig odooConfig;
    private final AttendanceRepository attendanceRepository;
    private final CurrentCompanyService currentCompanyService;

    public OdooSyncService(OdooConfig odooConfig, AttendanceRepository attendanceRepository, CurrentCompanyService currentCompanyService) {
        this.odooConfig = odooConfig;
        this.attendanceRepository = attendanceRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    public OdooSyncResult syncToday(Authentication authentication) {
        if (!odooConfig.isEnabled()) {
            return new OdooSyncResult(false, "Odoo sync is disabled (odoo.enabled=false)", 0);
        }

        Company company = currentCompanyService.requireCompany(authentication);
        Long companyId = company.getId();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant from = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<AttendanceRecord> records = attendanceRepository
                .findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(from, to, companyId);

        // TODO (real integration):
        // - map Employee to Odoo employee_id (hr.employee)
        // - upsert hr.attendance with check_in/check_out
        // - store sync status and errors per record
        // - handle retries and idempotency

        return new OdooSyncResult(true, "Odoo sync scaffold executed (no-op mapping not implemented yet)", records.size());
    }
}

