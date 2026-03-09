package com.online.attendance.billing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pesapal")
public class PesapalWebhookController {

    private final BillingService billingService;

    public PesapalWebhookController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(value = "OrderTrackingId", required = false) String orderTrackingId,
            @RequestParam(value = "OrderMerchantReference", required = false) String merchantReference
    ) {
        // Do not return JSON to the user according to Pesapal docs; but API clients expect JSON.
        // We return a simple body the frontend can use.
        return ResponseEntity.ok(Map.of(
                "orderTrackingId", orderTrackingId,
                "merchantReference", merchantReference
        ));
    }

    @RequestMapping(value = "/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> ipn(
            @RequestParam(value = "OrderTrackingId", required = false) String orderTrackingId,
            @RequestParam(value = "OrderMerchantReference", required = false) String merchantReference,
            @RequestParam(value = "OrderNotificationType", required = false) String notificationType
    ) {
        if (orderTrackingId == null || orderTrackingId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "OrderTrackingId is required"));
        }
        try {
            billingService.refreshPaymentStatus(orderTrackingId);
            return ResponseEntity.ok(Map.of(
                    "orderNotificationType", notificationType != null ? notificationType : "IPNCHANGE",
                    "orderTrackingId", orderTrackingId,
                    "orderMerchantReference", merchantReference,
                    "status", 200
            ));
        } catch (Exception ex) {
            return ResponseEntity.ok(Map.of(
                    "orderNotificationType", notificationType != null ? notificationType : "IPNCHANGE",
                    "orderTrackingId", orderTrackingId,
                    "orderMerchantReference", merchantReference,
                    "status", 500
            ));
        }
    }
}
