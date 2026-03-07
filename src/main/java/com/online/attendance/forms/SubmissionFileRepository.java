package com.online.attendance.forms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionFileRepository extends JpaRepository<SubmissionFile, Long> {
    List<SubmissionFile> findAllBySubmissionId(Long submissionId);
}
