package com.enterprise.order.service;

import com.enterprise.order.domain.Order;
import com.enterprise.order.domain.OrderStatus;
import com.enterprise.order.domain.OrderStatusHistory;
import com.enterprise.order.dto.*;
import com.enterprise.order.exception.InvalidOrderTransitionException;
import com.enterprise.order.exception.OrderNotFoundException;
import com.enterprise.order.repository.OrderRepository;
import com.enterprise.order.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderNumberGenerator orderNumberGenerator;

    /**
     * Creates a new order in PENDING status.
     * This is the entry point for the Saga flow.
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.generate())
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .currency("BRL")
                .correlationId(request.getCorrelationId())
                .build();

        request.getItems().forEach(itemRequest -> {
            com.enterprise.order.domain.OrderItem item = com.enterprise.order.domain.OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build();
            item.calculateSubtotal();
            order.getItems().add(item);
        });

        order.recalculateTotal();
        Order saved = orderRepository.save(order);

        log.info("Order created: {} | Customer: {} | Status: {}",
                saved.getOrderNumber(), saved.getCustomerId(), saved.getStatus());

        return toResponse(saved);
    }

    /**
     * Retrieves an order by ID.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("id", orderId.toString()));
        return toResponse(order);
    }

    /**
     * Retrieves an order by order number.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("orderNumber", orderNumber));
        return toResponse(order);
    }

    /**
     * Lists orders with optional status filter.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders = (status != null)
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return orders.map(this::toResponse);
    }

    /**
     * Lists orders for a specific customer.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrdersByCustomer(UUID customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    /**
     * Transitions an order to a new status.
     * Used by the Saga Orchestrator to drive the order lifecycle.
     */
    public OrderResponse updateStatus(UUID orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("id", orderId.toString()));

        OrderStatus fromStatus = order.getStatus();
        OrderStatus toStatus = request.getStatus();

        if (!fromStatus.canTransitionTo(toStatus)) {
            throw new InvalidOrderTransitionException(fromStatus, toStatus);
        }

        order.transitionTo(toStatus);

        if (request.getCorrelationId() != null) {
            order.setCorrelationId(request.getCorrelationId());
        }

        Order saved = orderRepository.save(order);

        recordTransition(saved, fromStatus, toStatus, request.getReason(), request.getCorrelationId());

        log.info("Order {} transitioned: {} → {} | Reason: {}",
                saved.getOrderNumber(), fromStatus, toStatus, request.getReason());

        return toResponse(saved);
    }

    /**
     * Links an order to a Saga instance.
     */
    public OrderResponse linkSaga(UUID orderId, String sagaInstanceId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("id", orderId.toString()));

        order.setSagaInstanceId(sagaInstanceId);
        Order saved = orderRepository.save(order);

        log.info("Order {} linked to Saga: {}", saved.getOrderNumber(), sagaInstanceId);
        return toResponse(saved);
    }

    /**
     * Sets the payment reference on an authorized order.
     */
    public OrderResponse setPaymentReference(UUID orderId, String paymentReference) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("id", orderId.toString()));

        order.setPaymentReference(paymentReference);
        Order saved = orderRepository.save(order);

        log.info("Order {} payment reference set: {}", saved.getOrderNumber(), paymentReference);
        return toResponse(saved);
    }

    /**
     * Retrieves the full status history of an order.
     */
    @Transactional(readOnly = true)
    public OrderHistoryResponse getOrderHistory(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("id", orderId.toString()));

        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrdered(orderId);

        List<OrderHistoryResponse.StatusTransition> transitions = history.stream()
                .map(h -> OrderHistoryResponse.StatusTransition.builder()
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .reason(h.getReason())
                        .correlationId(h.getCorrelationId())
                        .transitionedAt(h.getTransitionedAt())
                        .build())
                .toList();

        return OrderHistoryResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .transitions(transitions)
                .build();
    }

    private void recordTransition(Order order, OrderStatus from, OrderStatus to, String reason, String correlationId) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .fromStatus(from)
                .toStatus(to)
                .reason(reason)
                .correlationId(correlationId)
                .transitionedAt(LocalDateTime.now())
                .build();
        historyRepository.save(history);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .items(items)
                .correlationId(order.getCorrelationId())
                .paymentReference(order.getPaymentReference())
                .sagaInstanceId(order.getSagaInstanceId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .confirmedAt(order.getConfirmedAt())
                .cancelledAt(order.getCancelledAt())
                .build();
    }
}
