package com.online.attendance.billing.dto;

import com.online.attendance.billing.PesapalEnvironment;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PesapalSettingsResponse {
    private boolean enabled;
    private PesapalEnvironment environment;
    private String consumerKey;
    private String consumerSecretMasked;
    private String ipnId;
    private String ipnUrl;
    private String callbackUrl;
    private Instant updatedAt;
}
