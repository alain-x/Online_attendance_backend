package com.online.attendance.location;

import com.online.attendance.company.Company;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "work_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne
    @JoinColumn(name = "company_id", foreignKey = @ForeignKey(name = "fk_location_company"))
    private Company company;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "radius_meters", nullable = false)
    private int radiusMeters;

    @Column(nullable = false)
    private boolean active;
}
