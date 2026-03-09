package com.online.attendance.billing.dto;

import com.online.attendance.billing.SubscriptionStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionSummaryResponse {
    private SubscriptionStatus status;
    private Instant startAt;
    private Instant endAt;
    private Long planId;
    private String planName;
}
