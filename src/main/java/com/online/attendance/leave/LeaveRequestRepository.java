package com.online.attendance.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    LeaveRequest findByIdAndCompanyId(Long id, Long companyId);

    List<LeaveRequest> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<LeaveRequest> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, LeaveRequestStatus status);

    List<LeaveRequest> findByCompanyIdAndFromDateLessThanEqualAndToDateGreaterThanEqualAndStatus(Long companyId, LocalDate to, LocalDate from, LeaveRequestStatus status);

    List<LeaveRequest> findByEmployeeIdAndCompanyIdOrderByCreatedAtDesc(Long employeeId, Long companyId);
}
