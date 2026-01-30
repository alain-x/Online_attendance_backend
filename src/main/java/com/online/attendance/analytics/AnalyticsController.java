package com.online.attendance.analytics;

import com.online.attendance.attendance.AttendanceRecord;
import com.online.attendance.attendance.AttendanceRepository;
import com.online.attendance.attendance.BreakRecord;
import com.online.attendance.attendance.BreakRepository;
import com.online.attendance.company.Company;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.security.CurrentCompanyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final long STANDARD_WORKDAY_MINUTES = 8 * 60L;

    private final AttendanceRepository attendanceRepository;
    private final BreakRepository breakRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentCompanyService currentCompanyService;

    public AnalyticsController(
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

    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @GetMapping("/home")
    public HomeAnalyticsResponse home(Authentication authentication,
                                     @RequestParam(required = false) Integer year,
                                     @RequestParam(required = false) Integer month) {
        Company company = currentCompanyService.requireCompany(authentication);
        Long companyId = company.getId();

        long totalStaff = employeeRepository.countByUserCompanyId(companyId);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayFrom = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant todayTo = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<AttendanceRecord> todayRecords = attendanceRepository
                .findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(todayFrom, todayTo, companyId);

        Set<Long> presentEmployeeIds = new HashSet<>();
        Set<Long> checkedOutEmployeeIds = new HashSet<>();
        long locationNotVerifiedToday = 0;
        long faceNotVerifiedToday = 0;

        for (AttendanceRecord r : todayRecords) {
            if (r.getEmployee() != null) {
                presentEmployeeIds.add(r.getEmployee().getId());
            }
            if (r.getCheckOutTime() != null && r.getEmployee() != null) {
                checkedOutEmployeeIds.add(r.getEmployee().getId());
            }
            if (!r.isLocationVerified()) {
                locationNotVerifiedToday++;
            }
            if (!r.isFaceVerified()) {
                faceNotVerifiedToday++;
            }
        }

        long presentToday = presentEmployeeIds.size();
        long checkedOutToday = checkedOutEmployeeIds.size();
        long notInToday = Math.max(0, totalStaff - presentToday);

        YearMonth ym = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now(ZoneOffset.UTC);
        Instant monthFrom = ym.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant monthTo = ym.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<AttendanceRecord> monthRecords = attendanceRepository
                .findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(monthFrom, monthTo, companyId);

        long workedMinutesMonth = 0;
        Map<LocalDate, Long> countsByDay = new HashMap<>();
        for (AttendanceRecord r : monthRecords) {
            if (r.getCheckInTime() == null) {
                continue;
            }
            LocalDate d = LocalDate.ofInstant(r.getCheckInTime(), ZoneOffset.UTC);
            countsByDay.put(d, countsByDay.getOrDefault(d, 0L) + 1L);

            long breakMinutes = calculateBreakMinutes(r);
            long workedMinutes = calculateWorkedMinutes(r, breakMinutes);
            workedMinutesMonth += workedMinutes;
        }

        long overtimeMinutesMonth = calculateOvertimeMinutes(monthRecords);

        List<DailyCount> monthClockIns = new ArrayList<>();
        int daysInMonth = ym.lengthOfMonth();
        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate d = ym.atDay(i);
            long c = countsByDay.getOrDefault(d, 0L);
            monthClockIns.add(new DailyCount(d.toString(), c));
        }

        return new HomeAnalyticsResponse(
                totalStaff,
                presentToday,
                checkedOutToday,
                notInToday,
                locationNotVerifiedToday,
                faceNotVerifiedToday,
                workedMinutesMonth,
                overtimeMinutesMonth,
                monthClockIns
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
        List<BreakRecord> breaks = breakRepository.findByAttendanceRecordIdOrderByBreakStartTimeAsc(record.getId());
        return breaks.stream()
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

    /**
     * Approximate overtime as worked minutes beyond a standard workday
     * (8 hours) per attendance record.
     */
    private long calculateOvertimeMinutes(List<AttendanceRecord> monthRecords) {
        long overtime = 0;
        for (AttendanceRecord r : monthRecords) {
            long breakMinutes = calculateBreakMinutes(r);
            long workedMinutes = calculateWorkedMinutes(r, breakMinutes);
            long extra = workedMinutes - STANDARD_WORKDAY_MINUTES;
            if (extra > 0) {
                overtime += extra;
            }
        }
        return overtime;
    }
}
