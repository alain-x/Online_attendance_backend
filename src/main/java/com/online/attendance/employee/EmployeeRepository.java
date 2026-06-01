package com.online.attendance.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUserUsernameAndUserCompanyId(String username, Long companyId);
    Optional<Employee> findByUserIdAndUserCompanyId(Long userId, Long companyId);
    Optional<Employee> findByIdAndUserCompanyId(Long id, Long companyId);
    List<Employee> findByUserCompanyId(Long companyId);

    long countByUserCompanyId(Long companyId);

    @Query("SELECT e.id FROM Employee e WHERE e.user.id = :userId AND e.user.company.id = :companyId")
    Optional<Long> findEmployeeIdByUserIdAndCompanyId(@Param("userId") Long userId, @Param("companyId") Long companyId);

    interface ProfileImageView {
        byte[] getProfileImageBytes();
        String getProfileImageContentType();
    }

    @Query("SELECT e.profileImageBytes AS profileImageBytes, e.profileImageContentType AS profileImageContentType FROM Employee e WHERE e.id = :id")
    Optional<ProfileImageView> findProfileImageById(@Param("id") Long id);
}
