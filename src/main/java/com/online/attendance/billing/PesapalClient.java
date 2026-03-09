package com.online.attendance.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class PesapalClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiry;
    private volatile String cachedTokenKey;

    private String baseUrl(PesapalEnvironment env) {
        return env == PesapalEnvironment.LIVE ? "https://pay.pesapal.com/v3" : "https://cybqa.pesapal.com/pesapalv3";
    }

    public synchronized String getToken(PesapalEnvironment env, String consumerKey, String consumerSecret) throws IOException, InterruptedException {
        String tokenKey = (env != null ? env.name() : "") + ":" + (consumerKey != null ? consumerKey : "");
        if (cachedToken != null && cachedTokenExpiry != null && tokenKey.equals(cachedTokenKey)) {
            // refresh a bit early
            if (Instant.now().isBefore(cachedTokenExpiry.minusSeconds(15))) {
                return cachedToken;
            }
        }
        if (consumerKey == null || consumerKey.isBlank() || consumerSecret == null || consumerSecret.isBlank()) {
            throw new IllegalStateException("Pesapal consumer key/secret is not configured");
        }

        String url = baseUrl(env) + "/api/Auth/RequestToken";
        Map<String, Object> body = new HashMap<>();
        body.put("consumer_key", consumerKey);
        body.put("consumer_secret", consumerSecret);

        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Pesapal token request failed: HTTP " + res.statusCode() + " - " + res.body());
        }

        Map<?, ?> parsed = objectMapper.readValue(res.body(), Map.class);
        Object token = parsed.get("token");
        Object expiryDate = parsed.get("expiryDate");
        if (!(token instanceof String) || ((String) token).isBlank()) {
            throw new IllegalStateException("Pesapal token response missing token: " + res.body());
        }

        cachedToken = ((String) token).trim();
        cachedTokenKey = tokenKey;
        if (expiryDate instanceof String && !((String) expiryDate).isBlank()) {
            try {
                cachedTokenExpiry = Instant.parse(((String) expiryDate).trim());
            } catch (Exception ex) {
                cachedTokenExpiry = Instant.now().plusSeconds(240);
            }
        } else {
            cachedTokenExpiry = Instant.now().plusSeconds(240);
        }

        return cachedToken;
    }

    public Map<String, Object> registerIpn(PesapalEnvironment env, String consumerKey, String consumerSecret, String ipnUrl) throws IOException, InterruptedException {
        String token = getToken(env, consumerKey, consumerSecret);
        String url = baseUrl(env) + "/api/URLSetup/RegisterIPN";

        Map<String, Object> body = new HashMap<>();
        body.put("url", ipnUrl);
        body.put("ipn_notification_type", "GET");

        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Pesapal IPN register failed: HTTP " + res.statusCode() + " - " + res.body());
        }

        return objectMapper.readValue(res.body(), Map.class);
    }

    public Map<String, Object> submitOrder(
            PesapalEnvironment env,
            String consumerKey,
            String consumerSecret,
            String notificationId,
            String merchantReference,
            String description,
            String callbackUrl,
            double amount,
            String currency,
            Map<String, Object> billingAddress
    ) throws IOException, InterruptedException {
        String token = getToken(env, consumerKey, consumerSecret);
        String url = baseUrl(env) + "/api/Transactions/SubmitOrderRequest";

        Map<String, Object> body = new HashMap<>();
        body.put("id", merchantReference);
        body.put("currency", currency);
        body.put("amount", amount);
        body.put("description", description);
        body.put("callback_url", callbackUrl);
        body.put("notification_id", notificationId);
        body.put("billing_address", billingAddress);

        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Pesapal submit order failed: HTTP " + res.statusCode() + " - " + res.body());
        }

        return objectMapper.readValue(res.body(), Map.class);
    }

    public Map<String, Object> getTransactionStatus(PesapalEnvironment env, String consumerKey, String consumerSecret, String orderTrackingId) throws IOException, InterruptedException {
        String token = getToken(env, consumerKey, consumerSecret);
        String url = baseUrl(env) + "/api/Transactions/GetTransactionStatus?orderTrackingId="
                + URLEncoder.encode(orderTrackingId, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Pesapal transaction status failed: HTTP " + res.statusCode() + " - " + res.body());
        }

        return objectMapper.readValue(res.body(), Map.class);
    }
}
