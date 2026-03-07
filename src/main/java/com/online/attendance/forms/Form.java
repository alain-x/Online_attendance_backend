package com.online.attendance.forms;

import com.online.attendance.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "forms", indexes = {
        @Index(name = "idx_forms_company", columnList = "company_id"),
        @Index(name = "idx_forms_public_token", columnList = "public_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Form {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_forms_company"))
    private Company company;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "company_logo_url", length = 500)
    private String companyLogoUrl;

    @Column(name = "login_required", nullable = false)
    private boolean loginRequired;

    @Column(name = "public_enabled", nullable = false)
    private boolean publicEnabled;

    @Column(name = "public_token", length = 64)
    private String publicToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_storage_mode", nullable = false, length = 20)
    private FileStorageMode fileStorageMode;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
