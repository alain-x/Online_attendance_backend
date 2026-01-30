package com.online.attendance.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByCompanyIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long companyId, Instant from, Instant to);
}

