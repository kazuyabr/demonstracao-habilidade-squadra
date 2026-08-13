package com.enterprise.inventory.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks stock levels for a product in a specific warehouse/location.
 * A product can have stock in multiple locations.
 */
@Document(collection = "inventory_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    private String id;

    private String productId;

    private String warehouseId;

    /** Total quantity in stock */
    private Integer totalQuantity;

    /** Quantity currently reserved (pending orders) */
    private Integer reservedQuantity;

    /** Available = totalQuantity - reservedQuantity */
    private Integer availableQuantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalQuantity == null) totalQuantity = 0;
        if (reservedQuantity == null) reservedQuantity = 0;
        recalculateAvailable();
    }

    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        recalculateAvailable();
    }

    public void recalculateAvailable() {
        this.availableQuantity = this.totalQuantity - this.reservedQuantity;
    }

    public boolean hasEnoughStock(int quantity) {
        return this.availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!hasEnoughStock(quantity)) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s: requested %d, available %d",
                            productId, quantity, availableQuantity));
        }
        this.reservedQuantity += quantity;
        recalculateAvailable();
    }

    public void release(int quantity) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantity);
        recalculateAvailable();
    }
}
