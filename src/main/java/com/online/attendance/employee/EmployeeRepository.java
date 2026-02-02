package com.online.attendance.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUserUsernameAndUserCompanyId(String username, Long companyId);
    Optional<Employee> findByUserIdAndUserCompanyId(Long userId, Long companyId);
    Optional<Employee> findByIdAndUserCompanyId(Long id, Long companyId);
    List<Employee> findByUserCompanyId(Long companyId);

    long countByUserCompanyId(Long companyId);
}
