package com.online.attendance.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.attendance.billing.dto.CheckoutResponse;
import com.online.attendance.company.Company;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingService {

    private final SubscriptionPlanRepository planRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final PesapalSettingsRepository settingsRepository;
    private final PesapalClient pesapalClient;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BillingService(
            SubscriptionPlanRepository planRepository,
            CompanySubscriptionRepository subscriptionRepository,
            PaymentTransactionRepository paymentRepository,
            PesapalSettingsRepository settingsRepository,
            PesapalClient pesapalClient,
            UserRepository userRepository,
            CryptoService cryptoService
    ) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.settingsRepository = settingsRepository;
        this.pesapalClient = pesapalClient;
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
    }

    public PesapalSettings getOrCreateSettings() {
        return settingsRepository.findTopByOrderByIdAsc().orElseGet(() -> {
            PesapalSettings s = PesapalSettings.builder()
                    .enabled(false)
                    .environment(PesapalEnvironment.LIVE)
                    .updatedAt(Instant.now())
                    .build();
            return settingsRepository.save(s);
        });
    }

    private String requireConsumerKey(PesapalSettings settings) {
        String v = settings.getConsumerKey();
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Pesapal consumer key is not configured");
        }
        return v.trim();
    }

    private String requireConsumerSecret(PesapalSettings settings) {
        String enc = settings.getConsumerSecretEnc();
        if (enc == null || enc.isBlank()) {
            throw new IllegalStateException("Pesapal consumer secret is not configured");
        }
        String v = cryptoService.decryptFromBase64(enc);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Pesapal consumer secret is not configured");
        }
        return v;
    }

    @Transactional
    public CheckoutResponse startCheckout(Long companyId, Long planId, String companySlug, String username) throws Exception {
        PesapalSettings settings = getOrCreateSettings();
        if (!settings.isEnabled()) {
            throw new IllegalStateException("Payments are disabled");
        }
        if (settings.getIpnUrl() == null || settings.getIpnUrl().isBlank()) {
            throw new IllegalStateException("Pesapal IPN URL is not configured");
        }
        if (settings.getCallbackUrl() == null || settings.getCallbackUrl().isBlank()) {
            throw new IllegalStateException("Pesapal callback URL is not configured");
        }

        String consumerKey = requireConsumerKey(settings);
        String consumerSecret = requireConsumerSecret(settings);

        SubscriptionPlan plan = planRepository.findById(planId).orElseThrow(() -> new IllegalStateException("Plan not found"));
        if (!plan.isActive()) {
            throw new IllegalStateException("Plan is not active");
        }

        if (settings.getIpnId() == null || settings.getIpnId().isBlank()) {
            Map<String, Object> ipnRes = pesapalClient.registerIpn(settings.getEnvironment(), consumerKey, consumerSecret, settings.getIpnUrl());
            Object ipnId = ipnRes.get("ipn_id");
            if (ipnId instanceof String && !((String) ipnId).isBlank()) {
                settings.setIpnId(((String) ipnId).trim());
                settings.setUpdatedAt(Instant.now());
                settings = settingsRepository.save(settings);
            } else {
                throw new IllegalStateException("IPN registration failed: " + ipnRes);
            }
        }

        String merchantRef = "SUB-" + companyId + "-" + UUID.randomUUID().toString().replace("-", "");
        BigDecimal amount = plan.getPrice();

        Map<String, Object> billing = new HashMap<>();
        // optional fields; Pesapal docs require either email or phone. Our user model has email.
        AppUser u = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);
        if (u != null && u.getEmail() != null && !u.getEmail().isBlank()) {
            billing.put("email_address", u.getEmail().trim());
        }
        billing.put("country_code", "KE");
        billing.put("first_name", username);
        billing.put("last_name", companySlug);

        Map<String, Object> submitRes = pesapalClient.submitOrder(
                settings.getEnvironment(),
                consumerKey,
                consumerSecret,
                settings.getIpnId(),
                merchantRef,
                plan.getName() + " subscription",
                settings.getCallbackUrl(),
                amount.doubleValue(),
                plan.getCurrency() != null ? plan.getCurrency() : "KES",
                billing
        );

        String trackingId = submitRes.get("order_tracking_id") instanceof String ? (String) submitRes.get("order_tracking_id") : null;
        String redirectUrl = submitRes.get("redirect_url") instanceof String ? (String) submitRes.get("redirect_url") : null;
        String merchantReference = submitRes.get("merchant_reference") instanceof String ? (String) submitRes.get("merchant_reference") : merchantRef;
        if (trackingId == null || trackingId.isBlank() || redirectUrl == null || redirectUrl.isBlank()) {
            throw new IllegalStateException("Pesapal submit order did not return redirect_url/order_tracking_id: " + submitRes);
        }

        PaymentTransaction payment = PaymentTransaction.builder()
                .company(Company.builder().id(companyId).build())
                .plan(plan)
                .status(PaymentStatus.PENDING)
                .amount(amount)
                .currency(plan.getCurrency() != null ? plan.getCurrency() : "KES")
                .merchantReference(merchantReference)
                .orderTrackingId(trackingId)
                .rawStatusJson(objectMapper.writeValueAsString(submitRes))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        payment = paymentRepository.save(payment);

        CompanySubscription sub = subscriptionRepository.findByCompany_Id(companyId).orElse(null);
        if (sub == null) {
            sub = CompanySubscription.builder()
                    .company(Company.builder().id(companyId).build())
                    .plan(plan)
                    .status(SubscriptionStatus.PENDING_PAYMENT)
                    .updatedAt(Instant.now())
                    .build();
        } else {
            sub.setPlan(plan);
            sub.setStatus(SubscriptionStatus.PENDING_PAYMENT);
            sub.setUpdatedAt(Instant.now());
        }
        subscriptionRepository.save(sub);

        return CheckoutResponse.builder()
                .paymentId(payment.getId())
                .redirectUrl(redirectUrl)
                .orderTrackingId(trackingId)
                .merchantReference(merchantReference)
                .build();
    }

    @Transactional
    public PaymentTransaction refreshPaymentStatus(String orderTrackingId) throws Exception {
        PaymentTransaction tx = paymentRepository.findByOrderTrackingId(orderTrackingId).orElse(null);
        if (tx == null) {
            throw new IllegalStateException("Payment not found");
        }

        PesapalSettings settings = getOrCreateSettings();
        String consumerKey = requireConsumerKey(settings);
        String consumerSecret = requireConsumerSecret(settings);
        Map<String, Object> statusRes = pesapalClient.getTransactionStatus(settings.getEnvironment(), consumerKey, consumerSecret, orderTrackingId);
        tx.setRawStatusJson(objectMapper.writeValueAsString(statusRes));
        tx.setUpdatedAt(Instant.now());

        Integer statusCode = null;
        Object statusCodeObj = statusRes.get("status_code");
        if (statusCodeObj instanceof Number) {
            statusCode = ((Number) statusCodeObj).intValue();
        }

        // Pesapal docs: status_code is numeric; map non-0/1? We'll treat 1 as success, 2 as failed (sample shows 2=Failed).
        if (statusCode != null) {
            if (statusCode == 1) {
                tx.setStatus(PaymentStatus.COMPLETED);
            } else if (statusCode == 2) {
                tx.setStatus(PaymentStatus.FAILED);
            }
        }

        tx = paymentRepository.save(tx);

        if (tx.getStatus() == PaymentStatus.COMPLETED && tx.getCompany() != null && tx.getCompany().getId() != null) {
            Long companyId = tx.getCompany().getId();
            CompanySubscription sub = subscriptionRepository.findByCompany_Id(companyId).orElse(null);
            if (sub == null) {
                sub = CompanySubscription.builder()
                        .company(Company.builder().id(companyId).build())
                        .plan(tx.getPlan())
                        .status(SubscriptionStatus.ACTIVE)
                        .startAt(Instant.now())
                        .endAt(Instant.now().plusSeconds(30L * 24L * 3600L * (long) (tx.getPlan() != null ? tx.getPlan().getDurationMonths() : 1)))
                        .updatedAt(Instant.now())
                        .build();
            } else {
                sub.setPlan(tx.getPlan());
                sub.setStatus(SubscriptionStatus.ACTIVE);
                Instant now = Instant.now();
                sub.setStartAt(now);
                sub.setEndAt(now.plusSeconds(30L * 24L * 3600L * (long) (tx.getPlan() != null ? tx.getPlan().getDurationMonths() : 1)));
                sub.setUpdatedAt(Instant.now());
            }
            subscriptionRepository.save(sub);
        }

        return tx;
    }
}
