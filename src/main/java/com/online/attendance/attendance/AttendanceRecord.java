package com.online.attendance.attendance;

import com.online.attendance.employee.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "attendance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_employee"))
    private Employee employee;

    @Column(name = "check_in_time")
    private Instant checkInTime;

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    @Column(name = "check_in_lat")
    private Double checkInLat;

    @Column(name = "check_in_lng")
    private Double checkInLng;

    @Column(name = "check_out_lat")
    private Double checkOutLat;

    @Column(name = "check_out_lng")
    private Double checkOutLng;

    @Column(name = "location_verified", nullable = false)
    private boolean locationVerified;

    @Column(name = "face_verified", nullable = false)
    private boolean faceVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;
}
