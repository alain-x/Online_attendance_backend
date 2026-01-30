package com.online.attendance.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findTopByEmployeeUserUsernameAndEmployeeUserCompanyIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(String username, Long companyId);
    List<AttendanceRecord> findByEmployeeUserUsernameAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(String username, Long companyId);
    List<AttendanceRecord> findByCheckInTimeBetweenAndEmployeeUserCompanyIdOrderByCheckInTimeDesc(Instant from, Instant to, Long companyId);

    Optional<AttendanceRecord> findByIdAndEmployeeUserCompanyId(Long id, Long companyId);
}
