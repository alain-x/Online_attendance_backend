package com.online.attendance.forms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {
    List<FormSubmission> findAllByFormIdOrderBySubmittedAtDesc(Long formId);
    List<FormSubmission> findAllByCompany_IdOrderBySubmittedAtDesc(Long companyId);
}
