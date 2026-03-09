package com.online.attendance.billing.dto;

import com.online.attendance.billing.PesapalEnvironment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePesapalSettingsRequest {

    @NotNull
    private Boolean enabled;

    @NotNull
    private PesapalEnvironment environment;

    @Size(max = 200)
    private String consumerKey;

    @Size(max = 500)
    private String consumerSecret;

    @Size(max = 500)
    private String ipnUrl;

    @Size(max = 500)
    private String callbackUrl;
}
