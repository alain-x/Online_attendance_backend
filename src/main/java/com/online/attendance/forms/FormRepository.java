package com.online.attendance.forms;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Long> {
    @EntityGraph(attributePaths = {"company"})
    List<Form> findAllByCompany_IdOrderByUpdatedAtDesc(Long companyId);

    @EntityGraph(attributePaths = {"company"})
    Optional<Form> findByIdAndCompany_Id(Long id, Long companyId);

    Optional<Form> findByPublicToken(String publicToken);

    @org.springframework.data.jpa.repository.Query("SELECT f.company.id FROM Form f WHERE f.id = :id")
    Optional<Long> findCompanyIdById(@org.springframework.data.repository.query.Param("id") Long id);
}
