package com.online.attendance.company;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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

    /** Parent company (e.g. PRI). Null for root companies. Branches (e.g. PowerX, PowerM) reference the parent. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_company_id", foreignKey = @ForeignKey(name = "fk_company_parent"))
    @JsonIgnore
    private Company parentCompany;

    public Long getParentCompanyId() {
        return parentCompany != null ? parentCompany.getId() : null;
    }
}
