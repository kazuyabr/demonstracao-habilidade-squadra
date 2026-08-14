package com.enterprise.order.service;

import com.enterprise.events.producer.EventProducer;
import com.enterprise.order.domain.Order;
import com.enterprise.order.domain.OrderStatus;
import com.enterprise.order.dto.CreateOrderRequest;
import com.enterprise.order.dto.OrderItemRequest;
import com.enterprise.order.dto.OrderResponse;
import com.enterprise.order.dto.OrderStatusUpdateRequest;
import com.enterprise.order.exception.InvalidOrderTransitionException;
import com.enterprise.order.exception.OrderNotFoundException;
import com.enterprise.order.repository.OrderRepository;
import com.enterprise.order.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository historyRepository;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    @Mock
    private EventProducer eventProducer;

    @InjectMocks
    private OrderService orderService;

    private UUID orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-20260813-00001")
                .customerId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .currency("BRL")
                .totalAmount(new BigDecimal("100.00"))
                .build();
    }

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrder() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(UUID.randomUUID())
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-001")
                                .productName("Laptop")
                                .quantity(1)
                                .unitPrice(new BigDecimal("100.00"))
                                .build()
                ))
                .build();

        when(orderNumberGenerator.generate()).thenReturn("ORD-20260813-00001");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals("ORD-20260813-00001", response.getOrderNumber());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should get order by ID")
    void shouldGetOrderById() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals("ORD-20260813-00001", response.getOrderNumber());
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrder(UUID.randomUUID());
        });
    }

    @Test
    @DisplayName("Should transition order status")
    void shouldTransitionStatus() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(historyRepository.save(any())).thenReturn(null);

        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.PAYMENT_PROCESSING)
                .reason("Payment initiated")
                .build();

        OrderResponse response = orderService.updateStatus(orderId, request);

        assertNotNull(response);
        assertEquals(OrderStatus.PAYMENT_PROCESSING, response.getStatus());
        verify(historyRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw exception on invalid status transition")
    void shouldThrowOnInvalidTransition() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        assertThrows(InvalidOrderTransitionException.class, () -> {
            orderService.updateStatus(orderId, request);
        });
    }

    @Test
    @DisplayName("Should link saga to order")
    void shouldLinkSaga() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.linkSaga(orderId, "saga-123");

        assertNotNull(response);
        assertEquals("saga-123", response.getSagaInstanceId());
    }
}
