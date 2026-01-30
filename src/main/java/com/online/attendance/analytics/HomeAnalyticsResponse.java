package com.online.attendance.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class HomeAnalyticsResponse {
    private long totalStaff;
    private long presentToday;
    private long checkedOutToday;
    private long notInToday;

    private long locationNotVerifiedToday;
    private long faceNotVerifiedToday;

    private long workedMinutesMonth;
    private long overtimeMinutesMonth;

    private List<DailyCount> monthClockIns;
}
