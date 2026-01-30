package com.online.attendance.audit;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(Long companyId, String actorUsername, String action, String entityType, Long entityId, String details) {
        if (companyId == null || actorUsername == null || action == null) {
            return;
        }
        AuditLog log = AuditLog.builder()
                .companyId(companyId)
                .actorUsername(actorUsername)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(log);
    }
}

