package com.online.attendance.forms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Long> {
    List<Form> findAllByCompany_IdOrderByUpdatedAtDesc(Long companyId);
    Optional<Form> findByIdAndCompany_Id(Long id, Long companyId);
    Optional<Form> findByPublicToken(String publicToken);
}
