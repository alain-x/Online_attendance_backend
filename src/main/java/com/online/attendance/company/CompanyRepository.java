package com.online.attendance.company;

import com.online.attendance.company.dto.CompanyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    java.util.List<Company> findByParentCompany_Id(Long parentCompanyId);
    java.util.List<Company> findByParentCompanyIsNull();

    @Query("""
            SELECT new com.online.attendance.company.dto.CompanyResponse(
                c.id, c.name, c.slug, c.logoUrl, c.hourlyRateDefault, c.active, c.parentCompany.id)
            FROM Company c
            """)
    java.util.List<CompanyResponse> findAllResponses();

    @Query("""
            SELECT new com.online.attendance.company.dto.CompanyResponse(
                c.id, c.name, c.slug, c.logoUrl, c.hourlyRateDefault, c.active, c.parentCompany.id)
            FROM Company c WHERE c.id = :id
            """)
    Optional<CompanyResponse> findResponseById(@Param("id") Long id);

    @Query("""
            SELECT new com.online.attendance.company.dto.CompanyResponse(
                c.id, c.name, c.slug, c.logoUrl, c.hourlyRateDefault, c.active, c.parentCompany.id)
            FROM Company c WHERE c.parentCompany.id = :parentId
            """)
    java.util.List<CompanyResponse> findBranchResponsesByParentId(@Param("parentId") Long parentId);

    interface CompanyLogoView {
        byte[] getLogoBytes();
        String getLogoContentType();
    }

    @Query("SELECT c.logoBytes AS logoBytes, c.logoContentType AS logoContentType FROM Company c WHERE c.id = :id")
    Optional<CompanyLogoView> findLogoViewById(@Param("id") Long id);
}
