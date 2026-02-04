package com.online.attendance.holiday;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByCompanyIdOrderByDateAsc(Long companyId);

    List<Holiday> findByCompanyIdAndDateBetweenOrderByDateAsc(Long companyId, LocalDate from, LocalDate to);

    Holiday findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndDate(Long companyId, LocalDate date);

    boolean existsByCompanyIdAndDateAndIdNot(Long companyId, LocalDate date, Long id);
}
