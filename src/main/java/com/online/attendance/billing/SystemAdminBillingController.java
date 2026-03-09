package com.online.attendance.billing;

import com.online.attendance.billing.dto.UpdatePesapalSettingsRequest;
import com.online.attendance.billing.dto.UpsertSubscriptionPlanRequest;
import com.online.attendance.billing.dto.PesapalSettingsResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system-admin/billing")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemAdminBillingController {

    private final SubscriptionPlanRepository planRepository;
    private final PesapalSettingsRepository settingsRepository;
    private final BillingService billingService;
    private final CryptoService cryptoService;

    public SystemAdminBillingController(SubscriptionPlanRepository planRepository, PesapalSettingsRepository settingsRepository, BillingService billingService, CryptoService cryptoService) {
        this.planRepository = planRepository;
        this.settingsRepository = settingsRepository;
        this.billingService = billingService;
        this.cryptoService = cryptoService;
    }

    @GetMapping("/plans")
    public List<SubscriptionPlan> listPlans() {
        return planRepository.findAll();
    }

    @PostMapping("/plans")
    public ResponseEntity<?> createPlan(@Valid @RequestBody UpsertSubscriptionPlanRequest req) {
        SubscriptionPlan p = SubscriptionPlan.builder()
                .name(req.getName().trim())
                .price(req.getPrice())
                .durationMonths(req.getDurationMonths())
                .currency("KES")
                .active(Boolean.TRUE.equals(req.getActive()))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return ResponseEntity.ok(planRepository.save(p));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable Long id, @Valid @RequestBody UpsertSubscriptionPlanRequest req) {
        SubscriptionPlan p = planRepository.findById(id).orElse(null);
        if (p == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Plan not found"));
        }
        p.setName(req.getName().trim());
        p.setPrice(req.getPrice());
        p.setDurationMonths(req.getDurationMonths());
        p.setActive(Boolean.TRUE.equals(req.getActive()));
        p.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(planRepository.save(p));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable Long id) {
        SubscriptionPlan p = planRepository.findById(id).orElse(null);
        if (p == null) {
            return ResponseEntity.noContent().build();
        }
        planRepository.delete(p);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pesapal")
    public PesapalSettingsResponse getSettings() {
        PesapalSettings s = billingService.getOrCreateSettings();
        return toResponse(s);
    }

    @PutMapping("/pesapal")
    public PesapalSettingsResponse updateSettings(@Valid @RequestBody UpdatePesapalSettingsRequest req) {
        PesapalSettings s = billingService.getOrCreateSettings();
        s.setEnabled(Boolean.TRUE.equals(req.getEnabled()));
        s.setEnvironment(req.getEnvironment());
        if (req.getConsumerKey() != null) {
            String ck = req.getConsumerKey().trim();
            s.setConsumerKey(ck.isBlank() ? null : ck);
        }
        if (req.getConsumerSecret() != null) {
            String cs = req.getConsumerSecret().trim();
            s.setConsumerSecretEnc(cs.isBlank() ? null : cryptoService.encryptToBase64(cs));
        }
        s.setIpnUrl(req.getIpnUrl() != null && !req.getIpnUrl().trim().isBlank() ? req.getIpnUrl().trim() : null);
        s.setCallbackUrl(req.getCallbackUrl() != null && !req.getCallbackUrl().trim().isBlank() ? req.getCallbackUrl().trim() : null);
        s.setUpdatedAt(Instant.now());
        s = settingsRepository.save(s);
        return toResponse(s);
    }

    private PesapalSettingsResponse toResponse(PesapalSettings s) {
        String masked = null;
        if (s.getConsumerSecretEnc() != null && !s.getConsumerSecretEnc().isBlank()) {
            masked = "********";
        }
        return PesapalSettingsResponse.builder()
                .enabled(s.isEnabled())
                .environment(s.getEnvironment())
                .consumerKey(s.getConsumerKey())
                .consumerSecretMasked(masked)
                .ipnId(s.getIpnId())
                .ipnUrl(s.getIpnUrl())
                .callbackUrl(s.getCallbackUrl())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
