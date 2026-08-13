package com.enterprise.payment.service;

import com.enterprise.payment.domain.Payment;
import com.enterprise.payment.domain.PaymentMethod;
import com.enterprise.payment.domain.PaymentStatus;
import com.enterprise.payment.dto.AuthorizePaymentRequest;
import com.enterprise.payment.dto.PaymentResponse;
import com.enterprise.payment.dto.PaymentStatusUpdateRequest;
import com.enterprise.payment.exception.PaymentNotFoundException;
import com.enterprise.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGatewaySimulator gatewaySimulator;

    @InjectMocks
    private PaymentService paymentService;

    private UUID paymentId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        payment = Payment.builder()
                .id(paymentId)
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .idempotencyKey("idem-key-123")
                .build();
    }

    @Test
    @DisplayName("Should authorize payment successfully")
    void shouldAuthorizePayment() {
        AuthorizePaymentRequest request = AuthorizePaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .idempotencyKey("idem-key-456")
                .build();

        when(paymentRepository.findByIdempotencyKey("idem-key-456")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(gatewaySimulator.authorize(any())).thenReturn("GW-ABC123");

        PaymentResponse response = paymentService.authorizePayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
        assertEquals("GW-ABC123", response.getGatewayReference());
        verify(paymentRepository, times(3)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should return existing payment on idempotency key match")
    void shouldReturnExistingPaymentOnIdempotency() {
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setGatewayReference("GW-EXISTING");

        AuthorizePaymentRequest request = AuthorizePaymentRequest.builder()
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .idempotencyKey("idem-key-123")
                .build();

        when(paymentRepository.findByIdempotencyKey("idem-key-123")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.authorizePayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
        assertEquals("GW-EXISTING", response.getGatewayReference());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should get payment by ID")
    void shouldGetPaymentById() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPayment(paymentId);

        assertNotNull(response);
        assertEquals(paymentId, response.getId());
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void shouldThrowWhenPaymentNotFound() {
        when(paymentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.getPayment(UUID.randomUUID());
        });
    }

    @Test
    @DisplayName("Should request refund successfully")
    void shouldRequestRefund() {
        payment.setStatus(PaymentStatus.AUTHORIZED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.requestRefund(paymentId, "Inventory failed");

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals("Inventory failed", response.getFailureReason());
    }
}
