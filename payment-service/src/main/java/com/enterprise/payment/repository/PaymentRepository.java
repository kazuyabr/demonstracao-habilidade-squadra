package com.enterprise.payment.repository;

import com.enterprise.payment.domain.Payment;
import com.enterprise.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find payment by idempotency key.
     * This is the core of idempotent payment processing:
     * if a payment with this key exists, return it instead of creating a new one.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<Payment> findByOrderId(UUID orderId, Pageable pageable);

    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.status = :status")
    Optional<Payment> findByOrderIdAndStatus(@Param("orderId") UUID orderId, @Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.correlationId = :correlationId")
    Optional<Payment> findByCorrelationId(@Param("correlationId") String correlationId);
}
