package com.enterprise.payment.service;

import com.enterprise.payment.domain.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulates a payment gateway (e.g., Stripe, PagSeguro, Mercado Pago).
 *
 * In a real system, this would be replaced by an actual payment provider SDK.
 * For this project, we simulate:
 * - Successful authorizations (most of the time)
 * - Random failures (to demonstrate Saga compensation)
 * - Gateway reference generation
 *
 * This simulator is intentionally simple. The focus is on the Saga pattern,
 * not on payment gateway integration details.
 */
@Component
@Slf4j
public class PaymentGatewaySimulator {

    private static final double FAILURE_RATE = 0.1; // 10% failure rate

    /**
     * Simulates payment authorization.
     * Returns a gateway reference on success.
     * Throws RuntimeException on failure.
     */
    public String authorize(Payment payment) {
        log.info("Simulating payment authorization for payment: {} | Amount: {} {}",
                payment.getId(), payment.getAmount(), payment.getCurrency());

        // Simulate processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate random failures (10% of the time)
        if (Math.random() < FAILURE_RATE) {
            log.warn("Simulating payment failure for payment: {}", payment.getId());
            throw new RuntimeException("Payment gateway declined: insufficient funds or risk assessment failed");
        }

        String gatewayRef = "GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Payment authorized. Gateway reference: {}", gatewayRef);
        return gatewayRef;
    }

    /**
     * Simulates payment refund.
     */
    public void refund(Payment payment) {
        log.info("Simulating refund for payment: {} | Amount: {} {}",
                payment.getId(), payment.getAmount(), payment.getCurrency());

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Refund processed for payment: {}", payment.getId());
    }
}
