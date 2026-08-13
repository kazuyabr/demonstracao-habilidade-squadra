package com.enterprise.payment.service;

import com.enterprise.payment.domain.Payment;
import com.enterprise.payment.domain.PaymentStatus;
import com.enterprise.payment.dto.AuthorizePaymentRequest;
import com.enterprise.payment.dto.PaymentResponse;
import com.enterprise.payment.dto.PaymentStatusUpdateRequest;
import com.enterprise.payment.exception.InvalidPaymentTransitionException;
import com.enterprise.payment.exception.PaymentNotFoundException;
import com.enterprise.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewaySimulator gatewaySimulator;

    /**
     * Authorizes a payment with idempotency support.
     *
     * Idempotency flow:
     * 1. Check if a payment with this idempotencyKey already exists
     * 2. If yes, return the existing payment (no duplicate processing)
     * 3. If no, create and process the payment
     *
     * This is critical in distributed systems where network retries
     * can cause duplicate requests.
     */
    public PaymentResponse authorizePayment(AuthorizePaymentRequest request) {
        // Idempotency check: return existing payment if key already processed
        return paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(existing -> {
                    log.info("Payment already exists for idempotencyKey: {} | Returning existing payment: {}",
                            request.getIdempotencyKey(), existing.getId());
                    return toResponse(existing);
                })
                .orElseGet(() -> processNewPayment(request));
    }

    private PaymentResponse processNewPayment(AuthorizePaymentRequest request) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .idempotencyKey(request.getIdempotencyKey())
                .correlationId(request.getCorrelationId())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: {} | Order: {} | Amount: {} {} | IdempotencyKey: {}",
                saved.getId(), saved.getOrderId(), saved.getAmount(), saved.getCurrency(),
                saved.getIdempotencyKey());

        // Simulate payment gateway processing
        return processPayment(saved);
    }

    /**
     * Simulates calling a payment gateway.
     * In a real system, this would call an external payment provider (Stripe, PagSeguro, etc.)
     */
    private PaymentResponse processPayment(Payment payment) {
        try {
            payment.transitionTo(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            // Simulate gateway call with potential failure
            String gatewayRef = gatewaySimulator.authorize(payment);

            payment.setGatewayReference(gatewayRef);
            payment.transitionTo(PaymentStatus.AUTHORIZED);
            Payment saved = paymentRepository.save(payment);

            log.info("Payment authorized: {} | GatewayRef: {}", saved.getId(), gatewayRef);
            return toResponse(saved);

        } catch (Exception ex) {
            payment.setFailureReason(ex.getMessage());
            payment.transitionTo(PaymentStatus.FAILED);
            Payment saved = paymentRepository.save(payment);

            log.warn("Payment failed: {} | Reason: {}", saved.getId(), ex.getMessage());
            return toResponse(saved);
        }
    }

    /**
     * Gets a payment by ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("id", paymentId.toString()));
        return toResponse(payment);
    }

    /**
     * Gets a payment by idempotency key.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByIdempotencyKey(String idempotencyKey) {
        Payment payment = paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new PaymentNotFoundException("idempotencyKey", idempotencyKey));
        return toResponse(payment);
    }

    /**
     * Lists payments for an order.
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPaymentsByOrder(UUID orderId, Pageable pageable) {
        return paymentRepository.findByOrderId(orderId, pageable).map(this::toResponse);
    }

    /**
     * Updates payment status.
     * Used by the Saga Orchestrator for compensating transactions (refunds).
     */
    public PaymentResponse updateStatus(UUID paymentId, PaymentStatusUpdateRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("id", paymentId.toString()));

        PaymentStatus fromStatus = payment.getStatus();
        PaymentStatus toStatus = request.getStatus();

        if (!fromStatus.canTransitionTo(toStatus)) {
            throw new InvalidPaymentTransitionException(fromStatus, toStatus);
        }

        payment.transitionTo(toStatus);

        if (request.getGatewayReference() != null) {
            payment.setGatewayReference(request.getGatewayReference());
        }
        if (request.getReason() != null) {
            payment.setFailureReason(request.getReason());
        }
        if (request.getCorrelationId() != null) {
            payment.setCorrelationId(request.getCorrelationId());
        }

        Payment saved = paymentRepository.save(payment);

        log.info("Payment {} transitioned: {} → {} | Reason: {}",
                saved.getId(), fromStatus, toStatus, request.getReason());

        return toResponse(saved);
    }

    /**
     * Initiates a refund for an authorized payment.
     * This is the compensating transaction used by the Saga pattern
     * when inventory reservation fails after payment was authorized.
     */
    public PaymentResponse requestRefund(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("id", paymentId.toString()));

        if (!payment.getStatus().canTransitionTo(PaymentStatus.REFUND_REQUESTED)) {
            throw new InvalidPaymentTransitionException(payment.getStatus(), PaymentStatus.REFUND_REQUESTED);
        }

        payment.transitionTo(PaymentStatus.REFUND_REQUESTED);
        payment.setFailureReason(reason);
        Payment saved = paymentRepository.save(payment);

        // Simulate refund processing
        try {
            gatewaySimulator.refund(saved);
            saved.transitionTo(PaymentStatus.REFUNDED);
            Payment refunded = paymentRepository.save(saved);

            log.info("Payment refunded: {} | Reason: {}", refunded.getId(), reason);
            return toResponse(refunded);
        } catch (Exception ex) {
            log.error("Refund processing failed for payment: {} | Error: {}", saved.getId(), ex.getMessage());
            return toResponse(saved);
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .idempotencyKey(payment.getIdempotencyKey())
                .gatewayReference(payment.getGatewayReference())
                .failureReason(payment.getFailureReason())
                .correlationId(payment.getCorrelationId())
                .sagaInstanceId(payment.getSagaInstanceId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .authorizedAt(payment.getAuthorizedAt())
                .refundedAt(payment.getRefundedAt())
                .build();
    }
}
