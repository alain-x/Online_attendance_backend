package com.online.attendance.sports.club;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SportsClubRepository extends JpaRepository<SportsClub, Long> {
    Optional<SportsClub> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<SportsClub> findByCompanyId(Long companyId);
    List<SportsClub> findByCompanyIdAndActiveTrue(Long companyId);
    Optional<SportsClub> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsBySlugAndCompanyId(String slug, Long companyId);
}
