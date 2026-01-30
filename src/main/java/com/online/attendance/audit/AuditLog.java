package com.online.attendance.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_company_time", columnList = "company_id, created_at"),
                @Index(name = "idx_audit_actor_time", columnList = "actor_username, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "actor_username", nullable = false, length = 150)
    private String actorUsername;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Optional JSON/text payload. Keep it small (e.g. lat/lng, flags).
     */
    @Lob
    @Column(name = "details")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

