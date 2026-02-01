package com.online.attendance.reports;

import com.online.attendance.attendance.AttendanceRecord;
import com.online.attendance.attendance.AttendanceRepository;
import com.online.attendance.attendance.BreakRecord;
import com.online.attendance.attendance.BreakRepository;
import com.online.attendance.company.Company;
import com.online.attendance.security.CurrentCompanyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private final AttendanceRepository attendanceRepository;
    private final BreakRepository breakRepository;
    private final CurrentCompanyService currentCompanyService;

    public ReportsController(
            AttendanceRepository attendanceRepository,
            BreakRepository breakRepository,
            CurrentCompanyService currentCompanyService
    ) {
        this.attendanceRepository = attendanceRepository;
        this.breakRepository = breakRepository;
        this.currentCompanyService = currentCompanyService;
    }

    /**
     * Daily attendance summary as CSV.
     *
     * Columns:
     * date, employeeCode, firstName, lastName, checkInTimeUtc, checkOutTimeUtc,
     * locationVerified, faceVerified, status, breakMinutes, workedMinutes
     */
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @GetMapping(value = "/daily-attendance.csv", produces = "text/csv")
    public ResponseEntity<byte[]> dailyAttendanceCsv(
            Authentication authentication,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestParam(required = false) String date
    ) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Long effectiveCompanyId = company.getId();

        LocalDate day = (date == null || date.isBlank())
                ? LocalDate.now(ZoneOffset.UTC)
                : LocalDate.parse(date);

        Instant from = day.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<AttendanceRecord> records = attendanceRepository
                .findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(from, to, effectiveCompanyId);

        String csv = buildDailyAttendanceCsv(day, records);

        String fileName = "daily_attendance_" + company.getSlug() + "_" + day + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private String buildDailyAttendanceCsv(LocalDate day, List<AttendanceRecord> records) {
        // One line per employee per day.
        // IMPORTANT: Employees can have multiple records in a day (rare, but possible).
        // We aggregate to avoid wrong totals and to be export-correct.
        Map<Long, DailyEmployeeRow> perEmployee = new HashMap<>();

        for (AttendanceRecord r : records) {
            if (r.getEmployee() == null || r.getEmployee().getId() == null) {
                continue;
            }

            Long empId = r.getEmployee().getId();
            DailyEmployeeRow row = perEmployee.get(empId);
            if (row == null) {
                row = new DailyEmployeeRow();
                row.employeeCode = nz(r.getEmployee().getEmployeeCode());
                row.firstName = nz(r.getEmployee().getFirstName());
                row.lastName = nz(r.getEmployee().getLastName());
                row.locationVerified = true; // will be ANDed below
                row.faceVerified = true;     // will be ANDed below
                perEmployee.put(empId, row);
            }

            if (r.getCheckInTime() != null && (row.earliestCheckIn == null || r.getCheckInTime().isBefore(row.earliestCheckIn))) {
                row.earliestCheckIn = r.getCheckInTime();
            }
            if (r.getCheckOutTime() != null && (row.latestCheckOut == null || r.getCheckOutTime().isAfter(row.latestCheckOut))) {
                row.latestCheckOut = r.getCheckOutTime();
            }

            row.locationVerified = row.locationVerified && r.isLocationVerified();
            row.faceVerified = row.faceVerified && r.isFaceVerified();

            // Status: prefer PRESENT if any record is PRESENT, otherwise keep the first non-null.
            if (r.getStatus() != null) {
                if (row.status == null) {
                    row.status = r.getStatus().name();
                }
                if ("PRESENT".equals(r.getStatus().name())) {
                    row.status = "PRESENT";
                }
            }

            long breakMinutes = calculateBreakMinutesByAttendanceId(r.getId());
            long workedMinutes = calculateWorkedMinutes(r, breakMinutes);
            row.breakMinutes += breakMinutes;
            row.workedMinutes += workedMinutes;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("date,employeeCode,firstName,lastName,checkInTimeUtc,checkOutTimeUtc,locationVerified,faceVerified,status,breakMinutes,workedMinutes\n");

        perEmployee.values().stream()
                .sorted((a, b) -> a.employeeCode.compareToIgnoreCase(b.employeeCode))
                .forEach(row -> {
                    sb.append(csv(day.toString())).append(",");
                    sb.append(csv(row.employeeCode)).append(",");
                    sb.append(csv(row.firstName)).append(",");
                    sb.append(csv(row.lastName)).append(",");
                    sb.append(csv(row.earliestCheckIn != null ? row.earliestCheckIn.toString() : "")).append(",");
                    sb.append(csv(row.latestCheckOut != null ? row.latestCheckOut.toString() : "")).append(",");
                    sb.append(csv(String.valueOf(row.locationVerified))).append(",");
                    sb.append(csv(String.valueOf(row.faceVerified))).append(",");
                    sb.append(csv(row.status != null ? row.status : "")).append(",");
                    sb.append(csv(String.valueOf(row.breakMinutes))).append(",");
                    sb.append(csv(String.valueOf(row.workedMinutes))).append("\n");
                });

        return sb.toString();
    }

    private long calculateBreakMinutesByAttendanceId(Long attendanceId) {
        if (attendanceId == null) {
            return 0;
        }
        List<BreakRecord> breaks = breakRepository.findByAttendanceRecordIdOrderByBreakStartTimeAsc(attendanceId);
        return breaks.stream()
                .filter(b -> b.getBreakEndTime() != null)
                .mapToLong(b -> {
                    Duration d = Duration.between(b.getBreakStartTime(), b.getBreakEndTime());
                    return Math.max(0, d.toMinutes());
                })
                .sum();
    }

    private long calculateWorkedMinutes(AttendanceRecord record, long breakMinutes) {
        if (record.getCheckInTime() == null || record.getCheckOutTime() == null) {
            return 0;
        }
        Duration total = Duration.between(record.getCheckInTime(), record.getCheckOutTime());
        long totalMinutes = Math.max(0, total.toMinutes());
        return Math.max(0, totalMinutes - breakMinutes);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String csv(String s) {
        String v = s == null ? "" : s;
        v = v.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }

    private static class DailyEmployeeRow {
        String employeeCode;
        String firstName;
        String lastName;
        Instant earliestCheckIn;
        Instant latestCheckOut;
        boolean locationVerified;
        boolean faceVerified;
        String status;
        long breakMinutes;
        long workedMinutes;
    }
}

