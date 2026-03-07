package com.online.attendance.company;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "companies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_companies_slug", columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Lob
    @Column(name = "logo_bytes")
    @JsonIgnore
    private byte[] logoBytes;

    @Column(name = "logo_content_type", length = 100)
    @JsonIgnore
    private String logoContentType;

    @Column(name = "hourly_rate_default", precision = 12, scale = 2)
    private BigDecimal hourlyRateDefault;

    @Column(nullable = false)
    private boolean active = true;

    /** Parent company (e.g. PRI). Null for root companies. Branches (e.g. PowerX, PowerM) reference the parent. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_company_id", foreignKey = @ForeignKey(name = "fk_company_parent"))
    @JsonIgnore
    private Company parentCompany;

    public Long getParentCompanyId() {
        return parentCompany != null ? parentCompany.getId() : null;
    }
}
