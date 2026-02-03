package com.online.attendance.attendance;

import com.online.attendance.employee.Employee;
import com.online.attendance.user.AppUser;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "clock_out_type", length = 30)
    private ClockOutType clockOutType;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_purpose_status", length = 30)
    private CompanyPurposeStatus companyPurposeStatus;

    @Column(name = "company_purpose_note", columnDefinition = "TEXT")
    private String companyPurposeNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_purpose_approved_by_user_id", foreignKey = @ForeignKey(name = "fk_attendance_company_purpose_approved_by"))
    private AppUser companyPurposeApprovedBy;

    @Column(name = "company_purpose_approved_at")
    private Instant companyPurposeApprovedAt;

    @Column(name = "company_purpose_decision_note", columnDefinition = "TEXT")
    private String companyPurposeDecisionNote;

    @PrePersist
    @PreUpdate
    private void applyDefaults() {
        if (clockOutType == null) {
            clockOutType = ClockOutType.NORMAL;
        }
        if (companyPurposeStatus == null) {
            companyPurposeStatus = CompanyPurposeStatus.NONE;
        }
    }
}
