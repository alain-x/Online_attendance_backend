package com.online.attendance.billing.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {
    private Long paymentId;
    private String redirectUrl;
    private String orderTrackingId;
    private String merchantReference;
}
