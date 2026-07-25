package com.online.attendance.sports.sport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SportRepository extends JpaRepository<Sport, Long> {
    List<Sport> findByActiveTrue();
    List<Sport> findByCompanyId(Long companyId);
    List<Sport> findByCompanyIdAndActiveTrue(Long companyId);

    @Modifying
    @Query("UPDATE Sport s SET s.company.id = :companyId WHERE s.company IS NULL")
    int backfillCompanyId(Long companyId);

    @Query("SELECT COUNT(s) FROM Sport s WHERE s.company IS NULL")
    long countOrphaned();
}
