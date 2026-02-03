package com.online.attendance.attendance.dto;

import com.online.attendance.attendance.AttendanceStatus;
import com.online.attendance.attendance.ClockOutType;
import com.online.attendance.attendance.CompanyPurposeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class AttendanceResponse {
    private Long id;

    private Long employeeId;
    private String employeeCode;
    private String employeeFirstName;
    private String employeeLastName;

    private Instant checkInTime;
    private Instant checkOutTime;

    private Double checkInLat;
    private Double checkInLng;
    private Double checkOutLat;
    private Double checkOutLng;

    private boolean locationVerified;
    private boolean faceVerified;

    private AttendanceStatus status;

    private ClockOutType clockOutType;
    private CompanyPurposeStatus companyPurposeStatus;
    private String companyPurposeNote;

    /**
     * Total worked minutes for this attendance record.
     * This is the net duration between check-in and check-out minus approved breaks.
     */
    private long workedMinutes;

    /**
     * Total break minutes registered for this attendance record.
     */
    private long breakMinutes;
}
