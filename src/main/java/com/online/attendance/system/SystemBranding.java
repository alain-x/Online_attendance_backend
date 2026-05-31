package com.online.attendance.system;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "system_branding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemBranding {

    @Id
    @Column(length = 20)
    private String id;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "system_name", length = 200)
    private String systemName;

    @Column(name = "logo_path", length = 1000)
    private String logoPath;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "logo_bytes", columnDefinition = "BYTEA")
    private byte[] logoBytes;

    @Column(name = "logo_content_type", length = 120)
    private String logoContentType;

    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    @Column(name = "favicon_path", length = 1000)
    private String faviconPath;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "favicon_bytes", columnDefinition = "BYTEA")
    private byte[] faviconBytes;

    @Column(name = "favicon_content_type", length = 120)
    private String faviconContentType;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
