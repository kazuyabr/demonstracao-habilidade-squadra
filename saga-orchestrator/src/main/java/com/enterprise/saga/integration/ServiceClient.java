package com.enterprise.saga.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP client that calls the real microservices during saga execution.
 *
 * The orchestrator is the only component that talks to the services over the
 * Docker network (by container name) with plain HTTP - the API Gateway is not
 * involved, so no OAuth token is required for these internal calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${saga.services.payment-service}")
    private String paymentServiceUrl;

    @Value("${saga.services.inventory-service}")
    private String inventoryServiceUrl;

    @Value("${saga.services.order-service}")
    private String orderServiceUrl;

    /**
     * Result of an HTTP call: whether it succeeded and the response body (or error).
     */
    public record CallResult(boolean success, JsonNode body, String error) {}

    public CallResult authorizePayment(UUID orderId, UUID customerId, BigDecimal amount,
                                       String currency, String idempotencyKey, String correlationId) {
        Map<String, Object> body = Map.of(
                "orderId", orderId.toString(),
                "customerId", customerId.toString(),
                "amount", amount,
                "currency", currency != null ? currency : "BRL",
                "paymentMethod", "CREDIT_CARD",
                "idempotencyKey", idempotencyKey,
                "correlationId", correlationId
        );
        return post(paymentServiceUrl + "/api/v1/payments/authorize", body);
    }

    public CallResult refundPayment(String paymentId, String reason) {
        return post(paymentServiceUrl + "/api/v1/payments/" + paymentId + "/refund?reason=" + encode(reason), null);
    }

    public CallResult reserveInventory(String orderId, String productId, Integer quantity, String correlationId) {
        Map<String, Object> body = Map.of(
                "orderId", orderId,
                "productId", productId,
                "quantity", quantity,
                "correlationId", correlationId
        );
        return post(inventoryServiceUrl + "/api/v1/inventory/reserve", body);
    }

    public CallResult releaseInventory(String orderId, String productId) {
        String url = inventoryServiceUrl + "/api/v1/inventory/release?orderId=" + orderId
                + "&productId=" + productId + "&reason=" + encode("Saga compensation");
        return post(url, null);
    }

    public CallResult confirmOrder(String orderId, String correlationId) {
        return updateOrderStatus(orderId, "CONFIRMED", "Saga completed successfully", correlationId);
    }

    public CallResult cancelOrder(String orderId, String correlationId) {
        return updateOrderStatus(orderId, "CANCELLED", "Saga compensation", correlationId);
    }

    public CallResult updateOrderStatus(String orderId, String status, String reason, String correlationId) {
        Map<String, Object> body = Map.of(
                "status", status,
                "reason", reason != null ? reason : "",
                "correlationId", correlationId
        );
        return patch(orderServiceUrl + "/api/v1/orders/" + orderId + "/status", body);
    }

    // ------------------------------------------------------------------

    private CallResult post(String url, Object body) {
        return exchange(url, HttpMethod.POST, body);
    }

    private CallResult patch(String url, Object body) {
        return exchange(url, HttpMethod.PATCH, body);
    }

    private CallResult exchange(String url, HttpMethod method, Object body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(
                    body == null ? null : toJson(body), headers);
            log.info("{} {} | payload={}", method, url, body == null ? "{}" : toJson(body));
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
            JsonNode json = response.getBody() == null || response.getBody().isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response.getBody());
            log.info("Response {} {} | status={} | body={}", method, url, response.getStatusCode(), json);
            return new CallResult(response.getStatusCode().is2xxSuccessful(), json, null);
        } catch (HttpClientErrorException e) {
            log.warn("Client error {} {} | status={} | body={}", method, url, e.getStatusCode(), e.getResponseBodyAsString());
            return new CallResult(false, null, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Call failed {} {} | error={}", method, url, e.getMessage());
            return new CallResult(false, null, e.getMessage());
        }
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
