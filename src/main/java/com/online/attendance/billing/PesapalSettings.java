package com.online.attendance.billing;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pesapal_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PesapalSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PesapalEnvironment environment;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "consumer_key", length = 200)
    private String consumerKey;

    @Lob
    @Column(name = "consumer_secret_enc")
    private String consumerSecretEnc;

    @Column(name = "ipn_id", length = 80)
    private String ipnId;

    @Column(name = "ipn_url", length = 500)
    private String ipnUrl;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
