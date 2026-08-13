package com.enterprise.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Simulates sending notifications.
 * In a real system, this would integrate with:
 * - Email service (SendGrid, SES)
 * - SMS service (Twilio)
 * - Push notifications (Firebase)
 */
@Service
@Slf4j
public class NotificationService {

    public void sendOrderConfirmation(String orderNumber, String customerId) {
        log.info("📧 Sending order confirmation email | Order: {} | Customer: {}",
                orderNumber, customerId);
        // Simulate email sending
    }

    public void sendOrderCancellation(String orderNumber, String customerId, String reason) {
        log.info("📧 Sending order cancellation email | Order: {} | Customer: {} | Reason: {}",
                orderNumber, customerId, reason);
        // Simulate email sending
    }

    public void sendPaymentFailed(String orderNumber, String customerId, String reason) {
        log.info("📧 Sending payment failed email | Order: {} | Customer: {} | Reason: {}",
                orderNumber, customerId, reason);
        // Simulate email sending
    }

    public void sendRefundConfirmation(String orderNumber, String customerId) {
        log.info("📧 Sending refund confirmation email | Order: {} | Customer: {}",
                orderNumber, customerId);
        // Simulate email sending
    }
}
