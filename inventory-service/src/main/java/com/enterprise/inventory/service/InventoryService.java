package com.enterprise.inventory.service;

import com.enterprise.inventory.domain.*;
import com.enterprise.inventory.dto.InventoryItemResponse;
import com.enterprise.inventory.dto.ProductResponse;
import com.enterprise.inventory.dto.ReserveStockRequest;
import com.enterprise.inventory.dto.StockReservationResponse;
import com.enterprise.inventory.repository.InventoryItemRepository;
import com.enterprise.inventory.repository.ProductRepository;
import com.enterprise.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository reservationRepository;
    private final ProductRepository productRepository;

    /**
     * Reserves stock for an order.
     * This is called by the Saga Orchestrator after payment authorization.
     *
     * Flow:
     * 1. Check if enough stock is available
     * 2. Create a reservation (PENDING)
     * 3. Reserve stock in inventory item
     * 4. Confirm reservation
     */
    public StockReservationResponse reserveStock(ReserveStockRequest request) {
        // Check for existing reservation (idempotency)
        reservationRepository.findAllByOrderIdAndProductId(request.getOrderId(), request.getProductId()).stream()
                .findFirst()
                .ifPresent(existing -> {
                    if (existing.getStatus() != ReservationStatus.RELEASED) {
                        throw new IllegalStateException("Reservation already exists for order " +
                                request.getOrderId() + " and product " + request.getProductId());
                    }
                });

        // Find inventory item
        InventoryItem item = inventoryItemRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        // Create reservation
        StockReservation reservation = StockReservation.builder()
                .orderId(request.getOrderId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .status(ReservationStatus.PENDING)
                .reservationReference("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        try {
            // Reserve stock in inventory
            item.reserve(request.getQuantity());
            inventoryItemRepository.save(item);

            // Confirm reservation
            reservation.setStatus(ReservationStatus.CONFIRMED);
            StockReservation saved = reservationRepository.save(reservation);

            log.info("Stock reserved: Order {} | Product {} | Qty {} | Ref {}",
                    request.getOrderId(), request.getProductId(), request.getQuantity(),
                    saved.getReservationReference());

            return toReservationResponse(saved);

        } catch (InsufficientStockException ex) {
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setReason(ex.getMessage());
            reservationRepository.save(reservation);

            log.warn("Stock reservation failed: {} | Reason: {}", request.getOrderId(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Releases a reservation (compensating transaction).
     * Called when the Saga fails and the order needs to be cancelled.
     */
    public StockReservationResponse releaseStock(String orderId, String productId, String reason) {
        StockReservation reservation = reservationRepository
                .findAllByOrderIdAndProductId(orderId, productId).stream()
                .findFirst()
                .orElse(null);

        // Nothing was reserved (e.g. reservation never succeeded). Compensating
        // a reservation that does not exist is a no-op, so return 200 instead of
        // failing the whole compensation.
        if (reservation == null) {
            log.info("No reservation to release for Order {} | Product {} (no-op)", orderId, productId);
            return StockReservationResponse.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(0)
                    .status(ReservationStatus.RELEASED)
                    .reason(reason)
                    .build();
        }

        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            log.info("Reservation already released: Order {} | Product {}", orderId, productId);
            return toReservationResponse(reservation);
        }

        // Release stock in inventory
        InventoryItem item = inventoryItemRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        item.release(reservation.getQuantity());
        inventoryItemRepository.save(item);

        // Release reservation
        reservation.release(reason);
        StockReservation saved = reservationRepository.save(reservation);

        log.info("Stock released: Order {} | Product {} | Reason: {}", orderId, productId, reason);
        return toReservationResponse(saved);
    }

    /**
     * Gets a reservation by order and product.
     */
    public StockReservationResponse getReservation(String orderId, String productId) {
        StockReservation reservation = reservationRepository.findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new RuntimeException(
                        "Reservation not found for order " + orderId + " and product " + productId));
        return toReservationResponse(reservation);
    }

    /**
     * Gets inventory item for a product.
     */
    public InventoryItemResponse getInventoryItem(String productId) {
        InventoryItem item = inventoryItemRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        return toInventoryResponse(item);
    }

    /**
     * Gets all products.
     */
    public List<ProductResponse> getAllProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    private StockReservationResponse toReservationResponse(StockReservation reservation) {
        return StockReservationResponse.builder()
                .id(reservation.getId())
                .orderId(reservation.getOrderId())
                .productId(reservation.getProductId())
                .quantity(reservation.getQuantity())
                .status(reservation.getStatus())
                .reservationReference(reservation.getReservationReference())
                .reason(reservation.getReason())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .committedAt(reservation.getCommittedAt())
                .releasedAt(reservation.getReleasedAt())
                .build();
    }

    private InventoryItemResponse toInventoryResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .warehouseId(item.getWarehouseId())
                .totalQuantity(item.getTotalQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .availableQuantity(item.getAvailableQuantity())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .productType(product.getProductType())
                .price(product.getPrice())
                .currency(product.getCurrency())
                .attributes(product.getAttributes())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
