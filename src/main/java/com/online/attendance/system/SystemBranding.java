package com.online.attendance.system;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "updated_at")
    private Instant updatedAt;
}
