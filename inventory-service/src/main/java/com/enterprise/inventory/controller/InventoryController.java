package com.enterprise.inventory.controller;

import com.enterprise.inventory.dto.InventoryItemResponse;
import com.enterprise.inventory.dto.ProductResponse;
import com.enterprise.inventory.dto.ReserveStockRequest;
import com.enterprise.inventory.dto.StockReservationResponse;
import com.enterprise.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory API", description = "Stock reservation, release, and product catalog")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for an order",
            description = "Called by Saga Orchestrator after payment authorization")
    @ApiResponse(responseCode = "201", description = "Stock reserved")
    @ApiResponse(responseCode = "409", description = "Insufficient stock")
    public ResponseEntity<StockReservationResponse> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        StockReservationResponse response = inventoryService.reserveStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/release")
    @Operation(summary = "Release stock reservation",
            description = "Compensating transaction called by Saga Orchestrator when order is cancelled")
    public ResponseEntity<StockReservationResponse> releaseStock(
            @RequestParam String orderId,
            @RequestParam String productId,
            @RequestParam(default_value = "Saga compensation") String reason) {
        return ResponseEntity.ok(inventoryService.releaseStock(orderId, productId, reason));
    }

    @GetMapping("/reservation/{orderId}/{productId}")
    @Operation(summary = "Get reservation by order and product")
    public ResponseEntity<StockReservationResponse> getReservation(
            @PathVariable String orderId,
            @PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getReservation(orderId, productId));
    }

    @GetMapping("/item/{productId}")
    @Operation(summary = "Get inventory item for a product")
    public ResponseEntity<InventoryItemResponse> getInventoryItem(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getInventoryItem(productId));
    }

    @GetMapping("/products")
    @Operation(summary = "List all active products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(inventoryService.getAllProducts());
    }
}
