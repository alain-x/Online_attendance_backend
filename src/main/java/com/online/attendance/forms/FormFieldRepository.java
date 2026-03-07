package com.online.attendance.forms;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormFieldRepository extends JpaRepository<FormField, Long> {
    List<FormField> findAllByForm_IdOrderBySortOrderAsc(Long formId);
    void deleteAllByForm_Id(Long formId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from FormField f where f.form.id = :formId")
    int deleteByFormId(@Param("formId") Long formId);
}
