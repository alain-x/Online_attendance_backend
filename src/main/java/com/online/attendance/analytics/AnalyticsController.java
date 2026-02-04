package com.online.attendance.analytics;

import com.online.attendance.attendance.AttendanceRecord;
import com.online.attendance.attendance.AttendanceRepository;
import com.online.attendance.attendance.BreakRecord;
import com.online.attendance.attendance.BreakRepository;
import com.online.attendance.company.Company;
import com.online.attendance.employee.Employee;
import com.online.attendance.employee.EmployeeRepository;
import com.online.attendance.holiday.Holiday;
import com.online.attendance.holiday.HolidayRepository;
import com.online.attendance.leave.LeaveRequest;
import com.online.attendance.leave.LeaveRequestRepository;
import com.online.attendance.leave.LeaveRequestStatus;
import com.online.attendance.analytics.dto.DayAttendanceResponse;
import com.online.attendance.analytics.dto.DayEmployeeRow;
import com.online.attendance.analytics.dto.TimesheetCell;
import com.online.attendance.analytics.dto.TimesheetEmployeeRow;
import com.online.attendance.analytics.dto.TimesheetResponse;
import com.online.attendance.security.CurrentCompanyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
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
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final CurrentCompanyService currentCompanyService;

    public AnalyticsController(
            AttendanceRepository attendanceRepository,
            BreakRepository breakRepository,
            EmployeeRepository employeeRepository,
            HolidayRepository holidayRepository,
            LeaveRequestRepository leaveRequestRepository,
            CurrentCompanyService currentCompanyService
    ) {
        this.attendanceRepository = attendanceRepository;
        this.breakRepository = breakRepository;
        this.employeeRepository = employeeRepository;
        this.holidayRepository = holidayRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER')")
    @GetMapping("/home")
    public HomeAnalyticsResponse home(Authentication authentication,
                                     @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
                                     @RequestParam(required = false) Integer year,
                                     @RequestParam(required = false) Integer month) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Long effectiveCompanyId = company.getId();

        long totalStaff = employeeRepository.countByUserCompanyId(effectiveCompanyId);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayFrom = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant todayTo = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<AttendanceRecord> todayRecords = attendanceRepository
                .findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(todayFrom, todayTo, effectiveCompanyId);

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
                .findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(monthFrom, monthTo, effectiveCompanyId);

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

    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @GetMapping("/day")
    public DayAttendanceResponse day(
            Authentication authentication,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String roleScope,
            @RequestParam(required = false) String search
    ) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Long effectiveCompanyId = company.getId();

        LocalDate day = (date == null || date.isBlank())
                ? LocalDate.now(ZoneOffset.UTC)
                : LocalDate.parse(date);

        boolean isHoliday = holidayRepository.existsByCompanyIdAndDate(effectiveCompanyId, day);
        boolean isWeeklyOff = day.getDayOfWeek().getValue() == 7;
        Instant from = day.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Employee> allEmployees = employeeRepository.findByUserCompanyId(effectiveCompanyId);
        List<Employee> employees = filterEmployees(allEmployees, department, roleScope, search);

        List<AttendanceRecord> records = attendanceRepository
                .findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(from, to, effectiveCompanyId);

        Map<Long, DayAgg> perEmployee = new HashMap<>();
        for (AttendanceRecord r : records) {
            if (r.getEmployee() == null || r.getEmployee().getId() == null) continue;
            Long empId = r.getEmployee().getId();
            DayAgg agg = perEmployee.get(empId);
            if (agg == null) {
                agg = new DayAgg();
                perEmployee.put(empId, agg);
            }

            if (r.getCheckInTime() != null && (agg.earliestIn == null || r.getCheckInTime().isBefore(agg.earliestIn))) {
                agg.earliestIn = r.getCheckInTime();
            }
            if (r.getCheckOutTime() != null && (agg.latestOut == null || r.getCheckOutTime().isAfter(agg.latestOut))) {
                agg.latestOut = r.getCheckOutTime();
            }

            long breakMinutes = calculateBreakMinutes(r);
            long workedMinutes = calculateWorkedMinutes(r, breakMinutes);
            agg.breakMinutes += breakMinutes;
            agg.workedMinutes += workedMinutes;

            long extra = workedMinutes - STANDARD_WORKDAY_MINUTES;
            if (extra > 0) {
                agg.overtimeMinutes += extra;
            }
        }

        long present = 0;
        long workedMinutesTotal = 0;
        long overtimeMinutesTotal = 0;
        List<DayEmployeeRow> rows = new ArrayList<>();
        for (Employee e : employees) {
            DayAgg agg = perEmployee.get(e.getId());
            boolean isPresent = agg != null && agg.earliestIn != null;
            if (isPresent) present++;

            long worked = agg != null ? agg.workedMinutes : 0;
            long overtime = agg != null ? agg.overtimeMinutes : 0;
            workedMinutesTotal += worked;
            overtimeMinutesTotal += overtime;

            String status;
            if (!isPresent) {
                status = "NOT_IN";
            } else if (agg.latestOut != null) {
                status = "OUT";
            } else {
                status = "IN";
            }

            rows.add(new DayEmployeeRow(
                    e.getId(),
                    e.getEmployeeCode(),
                    e.getFirstName(),
                    e.getLastName(),
                    e.getDepartment(),
                    e.getUser() != null && e.getUser().getRole() != null ? e.getUser().getRole().name() : null,
                    agg != null && agg.earliestIn != null ? agg.earliestIn.toString() : null,
                    agg != null && agg.latestOut != null ? agg.latestOut.toString() : null,
                    worked,
                    overtime,
                    status
            ));
        }

        rows.sort((a, b) -> {
            String ac = a.getEmployeeCode() != null ? a.getEmployeeCode() : "";
            String bc = b.getEmployeeCode() != null ? b.getEmployeeCode() : "";
            return ac.compareToIgnoreCase(bc);
        });

        long totalStaff = employees.size();
        long notIn = Math.max(0, totalStaff - present);

        long holidays = 0;
        long weeklyOff = 0;
        if (isHoliday) {
            holidays = notIn;
        } else if (isWeeklyOff) {
            weeklyOff = notIn;
        }

        return new DayAttendanceResponse(
                day.toString(),
                totalStaff,
                present,
                notIn,
                holidays,
                weeklyOff,
                workedMinutesTotal,
                overtimeMinutesTotal,
                rows
        );
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN','HR','MANAGER')")
    @GetMapping("/timesheet")
    public TimesheetResponse timesheet(
            Authentication authentication,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String roleScope,
            @RequestParam(required = false) String search
    ) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Long effectiveCompanyId = company.getId();

        YearMonth ym = YearMonth.of(year, month);
        Instant from = ym.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = ym.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        LocalDate holidayFrom = ym.atDay(1);
        LocalDate holidayTo = ym.atEndOfMonth();
        List<Holiday> holidays = holidayRepository.findByCompanyIdAndDateBetweenOrderByDateAsc(effectiveCompanyId, holidayFrom, holidayTo);
        Set<LocalDate> holidayDates = new HashSet<>();
        for (Holiday h : holidays) {
            if (h.getDate() != null) {
                holidayDates.add(h.getDate());
            }
        }

        List<LeaveRequest> approvedLeave = leaveRequestRepository
                .findByCompanyIdAndFromDateLessThanEqualAndToDateGreaterThanEqualAndStatus(
                        effectiveCompanyId,
                        holidayTo,
                        holidayFrom,
                        LeaveRequestStatus.APPROVED
                );
        Map<Long, Set<LocalDate>> leaveDatesByEmployee = new HashMap<>();
        for (LeaveRequest lr : approvedLeave) {
            if (lr.getEmployee() == null || lr.getEmployee().getId() == null) continue;
            Long empId = lr.getEmployee().getId();

            LocalDate start = lr.getFromDate();
            LocalDate end = lr.getToDate();
            if (start == null || end == null) continue;

            LocalDate s = start.isBefore(holidayFrom) ? holidayFrom : start;
            LocalDate e = end.isAfter(holidayTo) ? holidayTo : end;
            if (s.isAfter(e)) continue;

            Set<LocalDate> set = leaveDatesByEmployee.computeIfAbsent(empId, k -> new HashSet<>());
            for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                set.add(d);
            }
        }

        List<Employee> allEmployees = employeeRepository.findByUserCompanyId(effectiveCompanyId);
        List<Employee> employees = filterEmployees(allEmployees, department, roleScope, search);

        List<AttendanceRecord> monthRecords = attendanceRepository
                .findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(from, to, effectiveCompanyId);

        Map<Long, Map<LocalDate, DayAgg>> perEmployeePerDay = new HashMap<>();
        for (AttendanceRecord r : monthRecords) {
            if (r.getEmployee() == null || r.getEmployee().getId() == null || r.getCheckInTime() == null) continue;
            Long empId = r.getEmployee().getId();
            LocalDate d = LocalDate.ofInstant(r.getCheckInTime(), ZoneOffset.UTC);

            Map<LocalDate, DayAgg> perDay = perEmployeePerDay.computeIfAbsent(empId, k -> new HashMap<>());
            DayAgg agg = perDay.get(d);
            if (agg == null) {
                agg = new DayAgg();
                perDay.put(d, agg);
            }

            if (r.getCheckInTime() != null && (agg.earliestIn == null || r.getCheckInTime().isBefore(agg.earliestIn))) {
                agg.earliestIn = r.getCheckInTime();
            }
            if (r.getCheckOutTime() != null && (agg.latestOut == null || r.getCheckOutTime().isAfter(agg.latestOut))) {
                agg.latestOut = r.getCheckOutTime();
            }

            long breakMinutes = calculateBreakMinutes(r);
            long workedMinutes = calculateWorkedMinutes(r, breakMinutes);
            agg.workedMinutes += workedMinutes;

            long extra = workedMinutes - STANDARD_WORKDAY_MINUTES;
            if (extra > 0) {
                agg.overtimeMinutes += extra;
            }
        }

        int daysInMonth = ym.lengthOfMonth();
        List<String> days = new ArrayList<>(daysInMonth);
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(ym.atDay(i).toString());
        }

        List<TimesheetEmployeeRow> rows = new ArrayList<>();
        for (Employee e : employees) {
            Map<LocalDate, DayAgg> perDay = perEmployeePerDay.getOrDefault(e.getId(), Collections.emptyMap());

            long presentDays = 0;
            long offDays = 0;
            long workedMinutesTotal = 0;
            long overtimeMinutesTotal = 0;
            long breakMinutesTotal = 0;

            List<TimesheetCell> cells = new ArrayList<>(daysInMonth);
            for (int i = 1; i <= daysInMonth; i++) {
                LocalDate d = ym.atDay(i);
                DayAgg agg = perDay.get(d);
                if (agg != null && agg.earliestIn != null) {
                    presentDays++;
                    workedMinutesTotal += agg.workedMinutes;
                    overtimeMinutesTotal += agg.overtimeMinutes;
                    breakMinutesTotal += agg.breakMinutes;
                    cells.add(new TimesheetCell("PRESENT", agg.workedMinutes, agg.overtimeMinutes, agg.breakMinutes));
                } else {
                    offDays++;
                    if (holidayDates.contains(d)) {
                        cells.add(new TimesheetCell("HOLIDAY", 0, 0, 0));
                    } else if (leaveDatesByEmployee.getOrDefault(e.getId(), Collections.emptySet()).contains(d)) {
                        cells.add(new TimesheetCell("LEAVE", 0, 0, 0));
                    } else {
                        cells.add(new TimesheetCell("OFF", 0, 0, 0));
                    }
                }
            }

            rows.add(new TimesheetEmployeeRow(
                    e.getId(),
                    e.getEmployeeCode(),
                    e.getFirstName(),
                    e.getLastName(),
                    e.getDepartment(),
                    e.getUser() != null && e.getUser().getRole() != null ? e.getUser().getRole().name() : null,
                    cells,
                    presentDays,
                    offDays,
                    workedMinutesTotal,
                    overtimeMinutesTotal,
                    breakMinutesTotal
            ));
        }

        rows.sort((a, b) -> {
            String ac = a.getEmployeeCode() != null ? a.getEmployeeCode() : "";
            String bc = b.getEmployeeCode() != null ? b.getEmployeeCode() : "";
            return ac.compareToIgnoreCase(bc);
        });

        return new TimesheetResponse(
                year,
                month,
                from.toString(),
                to.toString(),
                days,
                rows
        );
    }

    private static List<Employee> filterEmployees(
            List<Employee> employees,
            String department,
            String roleScope,
            String search
    ) {
        if (employees == null || employees.isEmpty()) return Collections.emptyList();

        String dep = department != null ? department.trim() : "";
        boolean depAll = dep.isEmpty() || "ALL".equalsIgnoreCase(dep);

        String scope = roleScope != null ? roleScope.trim() : "";
        boolean managersOnly = "MANAGERS".equalsIgnoreCase(scope);

        String q = search != null ? search.trim().toLowerCase() : "";

        List<Employee> out = new ArrayList<>();
        for (Employee e : employees) {
            if (!depAll) {
                String ed = e.getDepartment() != null ? e.getDepartment().trim() : "";
                if (!dep.equals(ed)) continue;
            }

            if (managersOnly) {
                if (e.getUser() == null || e.getUser().getRole() == null || !"MANAGER".equals(e.getUser().getRole().name())) {
                    continue;
                }
            }

            if (!q.isEmpty()) {
                String code = e.getEmployeeCode() != null ? e.getEmployeeCode() : "";
                String name = (e.getFirstName() != null ? e.getFirstName() : "") + " " + (e.getLastName() != null ? e.getLastName() : "");
                String hay = (code + " " + name).toLowerCase();
                if (!hay.contains(q)) continue;
            }

            out.add(e);
        }
        return out;
    }

    private static class DayAgg {
        Instant earliestIn;
        Instant latestOut;
        long workedMinutes;
        long overtimeMinutes;
        long breakMinutes;
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
