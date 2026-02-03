package com.online.attendance.payroll;

import com.online.attendance.attendance.AttendanceRecord;
import com.online.attendance.attendance.AttendanceRepository;
import com.online.attendance.attendance.ClockOutType;
import com.online.attendance.attendance.CompanyPurposeStatus;
import com.online.attendance.attendance.BreakRecord;
import com.online.attendance.attendance.BreakRepository;
import com.online.attendance.company.Company;
import com.online.attendance.employee.Employee;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.security.CurrentCompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final AttendanceRepository attendanceRepository;
    private final BreakRepository breakRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentCompanyService currentCompanyService;

    public PayrollController(
            AttendanceRepository attendanceRepository,
            BreakRepository breakRepository,
            EmployeeRepository employeeRepository,
            CurrentCompanyService currentCompanyService
    ) {
        this.attendanceRepository = attendanceRepository;
        this.breakRepository = breakRepository;
        this.employeeRepository = employeeRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER','PAYROLL','AUDITOR')")
    @GetMapping("/summary")
    public ResponseEntity<?> summary(
            Authentication authentication,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Long effectiveCompanyId = company.getId();

        Instant fromUtc = LocalDate.parse(from).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toUtc = LocalDate.parse(to).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<AttendanceRecord> records = attendanceRepository
                .findByCheckInTimeBetweenAndEmployeeUserCompanyIdAndCheckOutTimeIsNotNullOrderByCheckInTimeAsc(fromUtc, toUtc, effectiveCompanyId);

        BigDecimal companyDefaultRate = company.getHourlyRateDefault();

        Map<Long, PayrollRow> perEmployee = new HashMap<>();
        for (AttendanceRecord r : records) {
            if (r.getEmployee() == null || r.getEmployee().getId() == null) {
                continue;
            }

            if (!isPayable(r)) {
                continue;
            }

            long breakMinutes = calculateBreakMinutesByAttendanceId(r.getId());
            long workedMinutes = calculateWorkedMinutes(r, breakMinutes);

            if (workedMinutes <= 0) {
                continue;
            }

            Employee emp = r.getEmployee();
            PayrollRow row = perEmployee.get(emp.getId());
            if (row == null) {
                row = new PayrollRow();
                row.employeeId = emp.getId();
                row.employeeCode = emp.getEmployeeCode();
                row.firstName = emp.getFirstName();
                row.lastName = emp.getLastName();

                BigDecimal usedRate = emp.getHourlyRateOverride() != null ? emp.getHourlyRateOverride() : companyDefaultRate;
                row.hourlyRate = usedRate;
                perEmployee.put(emp.getId(), row);
            }

            row.workedMinutes += workedMinutes;
            LocalDate day = r.getCheckInTime().atZone(ZoneOffset.UTC).toLocalDate();
            row.workedMinutesByDay.merge(day, workedMinutes, Long::sum);
        }

        // Expected minutes are calculated for the requested period (Mon-Fri as work days).
        long expectedMinutesPerEmployee = calculateExpectedMinutes(from, to);

        List<PayrollRowResponse> rows = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        long totalRegularMinutes = 0;
        long totalOvertimeMinutes = 0;
        long totalExpectedMinutes = 0;
        long totalDeficitMinutes = 0;
        long totalMinutes = 0;

        for (PayrollRow r : perEmployee.values()) {
            WorkSplit split = splitRegularAndOvertime(r.workedMinutesByDay);
            long deficitMinutes = Math.max(0, expectedMinutesPerEmployee - split.regularMinutes);

            BigDecimal gross = calculateGross(r.workedMinutes, r.hourlyRate);
            BigDecimal net = calculateNet(gross);
            totalGross = totalGross.add(gross);
            totalNet = totalNet.add(net);
            totalMinutes += r.workedMinutes;
            totalRegularMinutes += split.regularMinutes;
            totalOvertimeMinutes += split.overtimeMinutes;
            totalExpectedMinutes += expectedMinutesPerEmployee;
            totalDeficitMinutes += deficitMinutes;

            rows.add(new PayrollRowResponse(
                    r.employeeId,
                    r.employeeCode,
                    r.firstName,
                    r.lastName,
                    r.workedMinutes,
                    expectedMinutesPerEmployee,
                    split.regularMinutes,
                    split.overtimeMinutes,
                    deficitMinutes,
                    r.hourlyRate,
                    gross,
                    net
            ));
        }

        PayrollSummaryResponse resp = new PayrollSummaryResponse(
                from,
                to,
                effectiveCompanyId,
                companyDefaultRate,
                totalMinutes,
                totalGross,
                totalNet,
                totalExpectedMinutes,
                totalRegularMinutes,
                totalOvertimeMinutes,
                totalDeficitMinutes,
                rows
        );

        return ResponseEntity.ok(resp);
    }

    private boolean isPayable(AttendanceRecord record) {
        ClockOutType type = record.getClockOutType() != null ? record.getClockOutType() : ClockOutType.NORMAL;
        CompanyPurposeStatus cp = record.getCompanyPurposeStatus() != null ? record.getCompanyPurposeStatus() : CompanyPurposeStatus.NONE;
        if (type == ClockOutType.COMPANY_PURPOSE) {
            return cp == CompanyPurposeStatus.APPROVED;
        }
        return true;
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
        long net = totalMinutes - breakMinutes;
        return Math.max(0, net);
    }

    private BigDecimal calculateGross(long workedMinutes, BigDecimal hourlyRate) {
        if (hourlyRate == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal minutes = new BigDecimal(workedMinutes);
        BigDecimal hours = minutes.divide(new BigDecimal(60), 4, RoundingMode.HALF_UP);
        return hours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateNet(BigDecimal grossPay) {
        if (grossPay == null) {
            return BigDecimal.ZERO;
        }
        return grossPay.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);
    }

    private long calculateExpectedMinutes(String from, String to) {
        LocalDate start = LocalDate.parse(from);
        LocalDate end = LocalDate.parse(to);
        long expected = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            boolean weekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            if (weekday) {
                expected += 8L * 60L;
            }
        }
        return expected;
    }

    // Rule 3:
    // - Daily overtime: minutes above 8h per day
    // - Weekly overtime: minutes above 40h per week, applied only to remaining regular minutes (avoid double counting daily OT)
    private WorkSplit splitRegularAndOvertime(Map<LocalDate, Long> workedMinutesByDay) {
        long totalWorked = 0;
        long dailyOvertime = 0;

        // Aggregate regular minutes by week based on a 8h/day cap.
        Map<Integer, Long> regularMinutesByWeek = new HashMap<>();
        for (Map.Entry<LocalDate, Long> e : workedMinutesByDay.entrySet()) {
            long dayWorked = e.getValue() != null ? e.getValue() : 0;
            if (dayWorked <= 0) continue;

            totalWorked += dayWorked;
            long dayOt = Math.max(0, dayWorked - (8L * 60L));
            dailyOvertime += dayOt;
            long dayRegularCapped = Math.min(dayWorked, 8L * 60L);

            int weekKey = weekKey(e.getKey());
            regularMinutesByWeek.merge(weekKey, dayRegularCapped, Long::sum);
        }

        long weeklyOvertime = 0;
        for (Long weekRegular : regularMinutesByWeek.values()) {
            long w = weekRegular != null ? weekRegular : 0;
            weeklyOvertime += Math.max(0, w - (40L * 60L));
        }

        long overtime = dailyOvertime + weeklyOvertime;
        long regular = Math.max(0, totalWorked - overtime);
        return new WorkSplit(regular, overtime);
    }

    private int weekKey(LocalDate d) {
        WeekFields wf = WeekFields.ISO;
        int week = d.get(wf.weekOfWeekBasedYear());
        int year = d.get(wf.weekBasedYear());
        return (year * 100) + week;
    }

    private static class PayrollRow {
        Long employeeId;
        String employeeCode;
        String firstName;
        String lastName;
        long workedMinutes;
        BigDecimal hourlyRate;
        Map<LocalDate, Long> workedMinutesByDay = new HashMap<>();
    }

    private record WorkSplit(long regularMinutes, long overtimeMinutes) {
    }

    public record PayrollRowResponse(
            Long employeeId,
            String employeeCode,
            String firstName,
            String lastName,
            long workedMinutes,
            long expectedMinutes,
            long regularMinutes,
            long overtimeMinutes,
            long deficitMinutes,
            BigDecimal hourlyRate,
            BigDecimal grossPay,
            BigDecimal netPay
    ) {
    }

    public record PayrollSummaryResponse(
            String from,
            String to,
            Long companyId,
            BigDecimal companyHourlyRateDefault,
            long totalWorkedMinutes,
            BigDecimal totalGrossPay,
            BigDecimal totalNetPay,
            long totalExpectedMinutes,
            long totalRegularMinutes,
            long totalOvertimeMinutes,
            long totalDeficitMinutes,
            List<PayrollRowResponse> rows
    ) {
    }
}
