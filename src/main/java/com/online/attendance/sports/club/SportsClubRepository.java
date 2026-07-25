package com.online.attendance.sports.club;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SportsClubRepository extends JpaRepository<SportsClub, Long> {
    Optional<SportsClub> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<SportsClub> findByCompanyId(Long companyId);
    List<SportsClub> findByCompanyIdAndActiveTrue(Long companyId);
    Optional<SportsClub> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsBySlugAndCompanyId(String slug, Long companyId);

    @Modifying
    @Query("UPDATE SportsClub c SET c.company.id = :companyId WHERE c.company IS NULL")
    int backfillCompanyId(Long companyId);

    @Query("SELECT COUNT(c) FROM SportsClub c WHERE c.company IS NULL")
    long countOrphaned();
}
