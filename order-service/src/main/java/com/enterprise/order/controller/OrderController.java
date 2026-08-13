package com.enterprise.order.controller;

import com.enterprise.order.domain.OrderStatus;
import com.enterprise.order.dto.CreateOrderRequest;
import com.enterprise.order.dto.OrderHistoryResponse;
import com.enterprise.order.dto.OrderResponse;
import com.enterprise.order.dto.OrderStatusUpdateRequest;
import com.enterprise.order.service.OrderService;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order API", description = "Operations for managing orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates an order in PENDING status. The Saga Orchestrator will drive the order through payment and inventory processing.")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping("/by-number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }

    @GetMapping
    @Operation(summary = "List all orders", description = "Supports filtering by status and pagination")
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @Parameter(description = "Filter by order status")
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.listOrders(status, pageable));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List orders by customer")
    public ResponseEntity<Page<OrderResponse>> listOrdersByCustomer(
            @PathVariable UUID customerId,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.listOrdersByCustomer(customerId, pageable));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status",
            description = "Used by the Saga Orchestrator to transition order states")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @ApiResponse(responseCode = "409", description = "Invalid state transition")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(orderId, request));
    }

    @PatchMapping("/{orderId}/saga")
    @Operation(summary = "Link order to Saga instance")
    public ResponseEntity<OrderResponse> linkSaga(
            @PathVariable UUID orderId,
            @RequestBody String sagaInstanceId) {
        return ResponseEntity.ok(orderService.linkSaga(orderId, sagaInstanceId));
    }

    @PatchMapping("/{orderId}/payment-reference")
    @Operation(summary = "Set payment reference on order")
    public ResponseEntity<OrderResponse> setPaymentReference(
            @PathVariable UUID orderId,
            @RequestBody String paymentReference) {
        return ResponseEntity.ok(orderService.setPaymentReference(orderId, paymentReference));
    }

    @GetMapping("/{orderId}/history")
    @Operation(summary = "Get order status history",
            description = "Shows the complete Saga trace for this order")
    public ResponseEntity<OrderHistoryResponse> getOrderHistory(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderHistory(orderId));
    }
}
