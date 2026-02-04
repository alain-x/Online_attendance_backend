package com.online.attendance.leave;

import com.online.attendance.company.Company;
import com.online.attendance.employee.Employee;
import com.online.attendance.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_leave_request_company"))
    private Company company;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_leave_request_employee"))
    private Employee employee;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_leave_request_leave_type"))
    private LeaveType leaveType;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_part", length = 10)
    private HalfDayPart halfDayPart;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id", foreignKey = @ForeignKey(name = "fk_leave_request_decided_by"))
    private AppUser decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = LeaveRequestStatus.PENDING;
        }
    }
}
