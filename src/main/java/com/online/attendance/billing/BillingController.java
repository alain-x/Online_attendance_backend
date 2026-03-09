package com.online.attendance.billing;

import com.online.attendance.billing.dto.CheckoutRequest;
import com.online.attendance.billing.dto.CheckoutResponse;
import com.online.attendance.billing.dto.SubscriptionSummaryResponse;
import com.online.attendance.company.Company;
import com.online.attendance.security.CurrentCompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final SubscriptionPlanRepository planRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final BillingService billingService;
    private final CurrentCompanyService currentCompanyService;

    public BillingController(
            SubscriptionPlanRepository planRepository,
            CompanySubscriptionRepository subscriptionRepository,
            PaymentTransactionRepository paymentRepository,
            BillingService billingService,
            CurrentCompanyService currentCompanyService
    ) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.billingService = billingService;
        this.currentCompanyService = currentCompanyService;
    }

    @GetMapping("/plans")
    public List<SubscriptionPlan> listActivePlans() {
        return planRepository.findAllByActiveTrueOrderByPriceAsc();
    }

    @GetMapping("/subscription")
    public SubscriptionSummaryResponse getMySubscription(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        CompanySubscription sub = subscriptionRepository.findByCompany_Id(company.getId()).orElse(null);
        if (sub == null) {
            return SubscriptionSummaryResponse.builder()
                    .status(SubscriptionStatus.INACTIVE)
                    .build();
        }
        return SubscriptionSummaryResponse.builder()
                .status(sub.getStatus())
                .startAt(sub.getStartAt())
                .endAt(sub.getEndAt())
                .planId(sub.getPlan() != null ? sub.getPlan().getId() : null)
                .planName(sub.getPlan() != null ? sub.getPlan().getName() : null)
                .build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId, @Valid @RequestBody CheckoutRequest req) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        String username = currentCompanyService.requireUsername(authentication);
        try {
            CheckoutResponse out = billingService.startCheckout(company.getId(), req.getPlanId(), companySlug, username);
            return ResponseEntity.ok(out);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Unable to start checkout"));
        }
    }

    @GetMapping("/payments/{trackingId}/refresh")
    public ResponseEntity<?> refreshPayment(Authentication authentication, @PathVariable String trackingId) {
        try {
            PaymentTransaction tx = billingService.refreshPaymentStatus(trackingId);
            return ResponseEntity.ok(tx);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Unable to refresh payment"));
        }
    }
}
