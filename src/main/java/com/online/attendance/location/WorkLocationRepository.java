package com.online.attendance.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkLocationRepository extends JpaRepository<WorkLocation, Long> {
    List<WorkLocation> findByActiveTrueAndCompanyId(Long companyId);
    List<WorkLocation> findByCompanyId(Long companyId);
    WorkLocation findByIdAndCompanyId(Long id, Long companyId);
}
