package com.jlapugot.inventoryservice.service;

import com.jlapugot.common.exceptions.BusinessException;
import com.jlapugot.common.exceptions.ResourceNotFoundException;
import com.jlapugot.common.model.OrderStatus;
import com.jlapugot.inventoryservice.model.Inventory;
import com.jlapugot.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inventory Service Tests")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .id(1L)
                .productId(100L)
                .productName("Test Product")
                .quantityAvailable(50)
                .quantityReserved(0)
                .reorderLevel(10)
                .build();
    }

    @Test
    @DisplayName("Should reserve inventory successfully")
    void shouldReserveInventorySuccessfully() {
        // Given
        Long productId = 100L;
        Integer quantity = 10;
        Long orderId = 1L;

        when(inventoryRepository.findByProductIdWithLock(productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        // When
        inventoryService.reserveInventory(productId, quantity, orderId);

        // Then
        verify(inventoryRepository).findByProductIdWithLock(productId);
        verify(inventoryRepository).save(any(Inventory.class));
        assertThat(inventory.getQuantityAvailable()).isEqualTo(40);
        assertThat(inventory.getQuantityReserved()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        Long productId = 999L;
        Integer quantity = 10;
        Long orderId = 1L;

        when(inventoryRepository.findByProductIdWithLock(productId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryService.reserveInventory(productId, quantity, orderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventory not found");

        verify(inventoryRepository).findByProductIdWithLock(productId);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void shouldThrowExceptionWhenInsufficientStock() {
        // Given
        Long productId = 100L;
        Integer quantity = 100; // More than available
        Long orderId = 1L;

        when(inventoryRepository.findByProductIdWithLock(productId))
                .thenReturn(Optional.of(inventory));

        // When & Then
        assertThatThrownBy(() -> inventoryService.reserveInventory(productId, quantity, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");

        verify(inventoryRepository).findByProductIdWithLock(productId);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should skip duplicate reservation for same order")
    void shouldSkipDuplicateReservation() {
        // Given
        Long productId = 100L;
        Integer quantity = 10;
        Long orderId = 1L;

        when(inventoryRepository.findByProductIdWithLock(productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        // First reservation
        inventoryService.reserveInventory(productId, quantity, orderId);

        // Reset mock to verify second call behavior
        reset(inventoryRepository);

        // When - Second reservation with same orderId
        inventoryService.reserveInventory(productId, quantity, orderId);

        // Then - Should not interact with repository for duplicate
        verify(inventoryRepository, never()).findByProductIdWithLock(anyLong());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should release reserved inventory successfully")
    void shouldReleaseReservedInventorySuccessfully() {
        // Given
        Long productId = 100L;
        Integer quantity = 10;
        Long orderId = 1L;

        // First reserve
        when(inventoryRepository.findByProductIdWithLock(productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        inventoryService.reserveInventory(productId, quantity, orderId);

        // When - Release
        inventoryService.releaseReservedInventory(orderId);

        // Then
        verify(inventoryRepository, times(2)).findByProductIdWithLock(productId);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(50); // Back to original
        assertThat(inventory.getQuantityReserved()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should skip release for non-existent reservation")
    void shouldSkipReleaseForNonExistentReservation() {
        // Given
        Long orderId = 999L;

        // When
        inventoryService.releaseReservedInventory(orderId);

        // Then - No exception thrown, just logs warning
        verify(inventoryRepository, never()).findByProductIdWithLock(anyLong());
    }

    @Test
    @DisplayName("Should confirm reserved inventory successfully")
    void shouldConfirmReservedInventorySuccessfully() {
        // Given
        Long productId = 100L;
        Integer quantity = 10;
        Long orderId = 1L;

        // First reserve
        when(inventoryRepository.findByProductIdWithLock(productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        inventoryService.reserveInventory(productId, quantity, orderId);

        // When - Confirm
        inventoryService.confirmReservedInventory(orderId);

        // Then
        verify(inventoryRepository, times(2)).findByProductIdWithLock(productId);
        assertThat(inventory.getQuantityReserved()).isEqualTo(0); // Reserved cleared
        assertThat(inventory.getQuantityAvailable()).isEqualTo(40); // Remains reduced
    }

    @Test
    @DisplayName("Should handle order status change from CONFIRMED to SHIPPED")
    void shouldHandleOrderStatusChangeToShipped() {
        // Given
        Long orderId = 1L;
        Long productId = 100L;
        Integer quantity = 10;

        // Reserve inventory first
        when(inventoryRepository.findByProductIdWithLock(productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        inventoryService.reserveInventory(productId, quantity, orderId);

        // When - Status changes to SHIPPED
        inventoryService.handleOrderStatusChange(orderId, OrderStatus.CONFIRMED, OrderStatus.SHIPPED);

        // Then - Should confirm the reservation
        verify(inventoryRepository, times(2)).findByProductIdWithLock(productId);
        assertThat(inventory.getQuantityReserved()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle order status change to CANCELLED")
    void shouldHandleOrderStatusChangeToCancelled() {
        // Given
        Long orderId = 1L;
        Long productId = 100L;
        Integer quantity = 10;

        // Reserve inventory first
        when(inventoryRepository.findByProductIdWithLock(productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        inventoryService.reserveInventory(productId, quantity, orderId);

        // When - Status changes to CANCELLED
        inventoryService.handleOrderStatusChange(orderId, OrderStatus.PENDING, OrderStatus.CANCELLED);

        // Then - Should release the reservation (calls findByProductIdWithLock twice: reserve + release)
        verify(inventoryRepository, times(2)).findByProductIdWithLock(productId);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(50); // Back to original
    }

    @Test
    @DisplayName("Should handle order status change to FAILED")
    void shouldHandleOrderStatusChangeToFailed() {
        // Given
        Long orderId = 1L;
        Long productId = 100L;
        Integer quantity = 10;

        // Reserve inventory first
        when(inventoryRepository.findByProductIdWithLock(productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        inventoryService.reserveInventory(productId, quantity, orderId);

        // When - Status changes to FAILED
        inventoryService.handleOrderStatusChange(orderId, OrderStatus.PENDING, OrderStatus.FAILED);

        // Then - Should release the reservation (calls findByProductIdWithLock twice: reserve + release)
        verify(inventoryRepository, times(2)).findByProductIdWithLock(productId);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should check if inventory needs reorder")
    void shouldCheckIfInventoryNeedsReorder() {
        // Given
        inventory.setQuantityAvailable(5);
        inventory.setQuantityReserved(0);
        // Total stock = 5 + 0 = 5, which is <= reorderLevel (10)

        // When
        boolean needsReorder = inventory.needsReorder();

        // Then
        assertThat(needsReorder).isTrue();
    }

    @Test
    @DisplayName("Should not need reorder when above reorder level")
    void shouldNotNeedReorderWhenAboveReorderLevel() {
        // Given
        inventory.setQuantityAvailable(50);
        inventory.setQuantityReserved(0);
        // Total stock = 50 + 0 = 50, which is > reorderLevel (10)

        // When
        boolean needsReorder = inventory.needsReorder();

        // Then
        assertThat(needsReorder).isFalse();
    }

    @Test
    @DisplayName("Should not need reorder when total stock (available + reserved) is above reorder level")
    void shouldNotNeedReorderWhenTotalStockAboveReorderLevel() {
        // Given
        inventory.setQuantityAvailable(3);
        inventory.setQuantityReserved(10);
        inventory.setReorderLevel(10);
        // Total stock = 3 + 10 = 13, which is > reorderLevel (10)

        // When
        boolean needsReorder = inventory.needsReorder();

        // Then
        assertThat(needsReorder).isFalse();
    }

    @Test
    @DisplayName("Should need reorder when total stock equals reorder level")
    void shouldNeedReorderWhenTotalStockEqualsReorderLevel() {
        // Given
        inventory.setQuantityAvailable(5);
        inventory.setQuantityReserved(5);
        inventory.setReorderLevel(10);
        // Total stock = 5 + 5 = 10, which is <= reorderLevel (10)

        // When
        boolean needsReorder = inventory.needsReorder();

        // Then
        assertThat(needsReorder).isTrue();
    }
}
