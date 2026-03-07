package com.online.attendance.forms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormFieldRepository extends JpaRepository<FormField, Long> {
    List<FormField> findAllByForm_IdOrderBySortOrderAsc(Long formId);
    void deleteAllByForm_Id(Long formId);
}
