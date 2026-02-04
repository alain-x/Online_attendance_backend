package com.online.attendance.leave;

import com.online.attendance.company.Company;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_types", uniqueConstraints = {
        @UniqueConstraint(name = "uk_leave_types_company_code", columnNames = {"company_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_leave_type_company"))
    private Company company;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private boolean paid;

    @Column(nullable = false)
    private boolean active = true;
}
