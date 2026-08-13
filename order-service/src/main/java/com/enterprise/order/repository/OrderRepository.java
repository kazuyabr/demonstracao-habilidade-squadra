package com.enterprise.order.repository;

import com.enterprise.order.domain.Order;
import com.enterprise.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.correlationId = :correlationId")
    Optional<Order> findByCorrelationId(@Param("correlationId") String correlationId);

    @Query("SELECT o FROM Order o WHERE o.sagaInstanceId = :sagaInstanceId")
    Optional<Order> findBySagaInstanceId(@Param("sagaInstanceId") String sagaInstanceId);
}
