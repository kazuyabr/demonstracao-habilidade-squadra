package com.enterprise.inventory.service;

import com.enterprise.inventory.domain.*;
import com.enterprise.inventory.dto.ReserveStockRequest;
import com.enterprise.inventory.dto.StockReservationResponse;
import com.enterprise.inventory.repository.InventoryItemRepository;
import com.enterprise.inventory.repository.ProductRepository;
import com.enterprise.inventory.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        inventoryItem = InventoryItem.builder()
                .id("item-001")
                .productId("PROD-001")
                .warehouseId("WH-01")
                .totalQuantity(100)
                .reservedQuantity(0)
                .build();
        inventoryItem.recalculateAvailable();
    }

    @Test
    @DisplayName("Should reserve stock successfully")
    void shouldReserveStock() {
        ReserveStockRequest request = ReserveStockRequest.builder()
                .orderId("order-123")
                .productId("PROD-001")
                .quantity(10)
                .build();

        when(reservationRepository.findByOrderIdAndProductId("order-123", "PROD-001"))
                .thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> {
            StockReservation r = inv.getArgument(0);
            r.setId("res-001");
            return r;
        });

        StockReservationResponse response = inventoryService.reserveStock(request);

        assertNotNull(response);
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
        assertEquals(10, inventoryItem.getReservedQuantity());
        assertEquals(90, inventoryItem.getAvailableQuantity());
    }

    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void shouldThrowOnInsufficientStock() {
        ReserveStockRequest request = ReserveStockRequest.builder()
                .orderId("order-456")
                .productId("PROD-001")
                .quantity(200)
                .build();

        when(reservationRepository.findByOrderIdAndProductId("order-456", "PROD-001"))
                .thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(InsufficientStockException.class, () -> {
            inventoryService.reserveStock(request);
        });

        verify(reservationRepository, times(1)).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("Should release stock successfully")
    void shouldReleaseStock() {
        inventoryItem.reserve(20);
        when(inventoryItemRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));

        StockReservation reservation = StockReservation.builder()
                .id("res-001")
                .orderId("order-123")
                .productId("PROD-001")
                .quantity(20)
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findByOrderIdAndProductId("order-123", "PROD-001"))
                .thenReturn(Optional.of(reservation));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        StockReservationResponse response = inventoryService.releaseStock("order-123", "PROD-001", "Saga compensation");

        assertNotNull(response);
        assertEquals(ReservationStatus.RELEASED, response.getStatus());
        assertEquals(0, inventoryItem.getReservedQuantity());
        assertEquals(100, inventoryItem.getAvailableQuantity());
    }
}
