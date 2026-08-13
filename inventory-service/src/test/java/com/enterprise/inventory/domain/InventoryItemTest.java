package com.enterprise.inventory.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryItemTest {

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = InventoryItem.builder()
                .id("item-001")
                .productId("PROD-001")
                .warehouseId("WH-01")
                .totalQuantity(100)
                .reservedQuantity(0)
                .build();
        item.recalculateAvailable();
    }

    @Test
    @DisplayName("Should calculate available quantity correctly")
    void shouldCalculateAvailable() {
        assertEquals(100, item.getAvailableQuantity());
    }

    @Test
    @DisplayName("Should reserve stock successfully")
    void shouldReserveStock() {
        item.reserve(30);
        assertEquals(30, item.getReservedQuantity());
        assertEquals(70, item.getAvailableQuantity());
    }

    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void shouldThrowOnInsufficientStock() {
        assertThrows(InsufficientStockException.class, () -> {
            item.reserve(150);
        });
    }

    @Test
    @DisplayName("Should release stock successfully")
    void shouldReleaseStock() {
        item.reserve(50);
        assertEquals(50, item.getReservedQuantity());

        item.release(30);
        assertEquals(20, item.getReservedQuantity());
        assertEquals(80, item.getAvailableQuantity());
    }

    @Test
    @DisplayName("Should not release below zero")
    void shouldNotReleaseBelowZero() {
        item.reserve(10);
        item.release(50);
        assertEquals(0, item.getReservedQuantity());
        assertEquals(100, item.getAvailableQuantity());
    }

    @Test
    @DisplayName("Should check if enough stock is available")
    void shouldCheckEnoughStock() {
        assertTrue(item.hasEnoughStock(100));
        assertTrue(item.hasEnoughStock(1));
        assertFalse(item.hasEnoughStock(101));
    }
}
