package com.online.attendance.attendance;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "break_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreakRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "attendance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_break_attendance"))
    private AttendanceRecord attendanceRecord;

    @Column(name = "break_start_time", nullable = false)
    private Instant breakStartTime;

    @Column(name = "break_end_time")
    private Instant breakEndTime;
}
