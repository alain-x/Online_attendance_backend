package com.online.attendance.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    java.util.List<Company> findByParentCompany_Id(Long parentCompanyId);
    java.util.List<Company> findByParentCompanyIsNull();
}
