package com.online.attendance.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DailyCount {
    private String day;
    private long count;
}
