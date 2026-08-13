package com.enterprise.payment.controller;

import com.enterprise.payment.dto.AuthorizePaymentRequest;
import com.enterprise.payment.dto.PaymentResponse;
import com.enterprise.payment.dto.PaymentStatusUpdateRequest;
import com.enterprise.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "Payment authorization, refund, and idempotent processing")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/authorize")
    @Operation(summary = "Authorize a payment",
            description = "Authorizes a payment with idempotency support. Duplicate requests with the same idempotencyKey return the existing payment.")
    @ApiResponse(responseCode = "201", description = "Payment authorized successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public ResponseEntity<PaymentResponse> authorizePayment(@Valid @RequestBody AuthorizePaymentRequest request) {
        PaymentResponse response = paymentService.authorizePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    @GetMapping("/by-idempotency-key/{idempotencyKey}")
    @Operation(summary = "Get payment by idempotency key",
            description = "Used to check if a payment was already processed")
    public ResponseEntity<PaymentResponse> getByIdempotencyKey(@PathVariable String idempotencyKey) {
        return ResponseEntity.ok(paymentService.getPaymentByIdempotencyKey(idempotencyKey));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "List payments for an order")
    public ResponseEntity<Page<PaymentResponse>> listPaymentsByOrder(
            @PathVariable UUID orderId,
            Pageable pageable) {
        return ResponseEntity.ok(paymentService.listPaymentsByOrder(orderId, pageable));
    }

    @PatchMapping("/{paymentId}/status")
    @Operation(summary = "Update payment status",
            description = "Used by Saga Orchestrator for status transitions and refunds")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "409", description = "Invalid transition")
    public ResponseEntity<PaymentResponse> updateStatus(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentStatusUpdateRequest request) {
        return ResponseEntity.ok(paymentService.updateStatus(paymentId, request));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Request refund",
            description = "Compensating transaction for when inventory fails after payment authorization")
    public ResponseEntity<PaymentResponse> requestRefund(
            @PathVariable UUID paymentId,
            @RequestParam(defaultValue = "Saga compensation: inventory reservation failed") String reason) {
        return ResponseEntity.ok(paymentService.requestRefund(paymentId, reason));
    }
}
