package com.online.attendance.holiday;

import com.online.attendance.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "holidays", uniqueConstraints = {
        @UniqueConstraint(name = "uk_holidays_company_date", columnNames = {"company_id", "holiday_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_holiday_company"))
    private Company company;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 200)
    private String name;
}
