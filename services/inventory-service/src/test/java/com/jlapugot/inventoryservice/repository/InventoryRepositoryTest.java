package com.jlapugot.inventoryservice.repository;

import com.jlapugot.inventoryservice.model.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Inventory Repository Tests")
@Import(com.jlapugot.inventoryservice.config.JpaConfig.class)
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    private Inventory inventory1;
    private Inventory inventory2;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();

        inventory1 = Inventory.builder()
                .productId(100L)
                .productName("Product 1")
                .quantityAvailable(50)
                .quantityReserved(10)
                .reorderLevel(10)
                .build();

        inventory2 = Inventory.builder()
                .productId(200L)
                .productName("Product 2")
                .quantityAvailable(100)
                .quantityReserved(0)
                .reorderLevel(20)
                .build();

        inventoryRepository.save(inventory1);
        inventoryRepository.save(inventory2);
    }

    @Test
    @DisplayName("Should find inventory by product ID")
    void shouldFindInventoryByProductId() {
        // When
        Optional<Inventory> result = inventoryRepository.findByProductId(100L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo(100L);
        assertThat(result.get().getQuantityAvailable()).isEqualTo(50);
        assertThat(result.get().getQuantityReserved()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should return empty when product not found")
    void shouldReturnEmptyWhenProductNotFound() {
        // When
        Optional<Inventory> result = inventoryRepository.findByProductId(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find inventory by product ID with pessimistic lock")
    void shouldFindInventoryByProductIdWithLock() {
        // When
        Optional<Inventory> result = inventoryRepository.findByProductIdWithLock(100L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo(100L);
        assertThat(result.get().getQuantityAvailable()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should save and retrieve inventory")
    void shouldSaveAndRetrieveInventory() {
        // Given
        Inventory newInventory = Inventory.builder()
                .productId(300L)
                .productName("Product 3")
                .quantityAvailable(75)
                .quantityReserved(5)
                .reorderLevel(15)
                .build();

        // When
        Inventory saved = inventoryRepository.save(newInventory);
        Optional<Inventory> retrieved = inventoryRepository.findById(saved.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProductId()).isEqualTo(300L);
        assertThat(retrieved.get().getQuantityAvailable()).isEqualTo(75);
        assertThat(retrieved.get().getQuantityReserved()).isEqualTo(5);
        assertThat(retrieved.get().getReorderLevel()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should update inventory quantities")
    void shouldUpdateInventoryQuantities() {
        // Given
        Inventory inventory = inventoryRepository.findByProductId(100L).orElseThrow();

        // When
        inventory.reserveQuantity(20);
        inventoryRepository.save(inventory);

        // Then
        Inventory updated = inventoryRepository.findByProductId(100L).orElseThrow();
        assertThat(updated.getQuantityAvailable()).isEqualTo(30); // 50 - 20
        assertThat(updated.getQuantityReserved()).isEqualTo(30);  // 10 + 20
    }

    @Test
    @DisplayName("Should handle concurrent updates with version")
    void shouldHandleConcurrentUpdatesWithVersion() {
        // Given
        Inventory inventory = inventoryRepository.findByProductId(100L).orElseThrow();
        Long initialVersion = inventory.getVersion();

        // When
        inventory.reserveQuantity(10);
        inventoryRepository.save(inventory);

        // Then
        Inventory updated = inventoryRepository.findByProductId(100L).orElseThrow();
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }

    @Test
    @DisplayName("Should check if inventory needs reorder")
    void shouldCheckIfInventoryNeedsReorder() {
        // Given
        Inventory inventory = inventoryRepository.findByProductId(100L).orElseThrow();
        inventory.setQuantityAvailable(3); // 3 available + 10 reserved = 13, but we need <= 10
        inventory.setQuantityReserved(0); // Clear reserved so total is 3 which is < 10

        // When
        boolean needsReorder = inventory.needsReorder();

        // Then
        assertThat(needsReorder).isTrue();
    }

    @Test
    @DisplayName("Should have audit timestamps")
    void shouldHaveAuditTimestamps() {
        // Given
        Inventory inventory = inventoryRepository.findByProductId(100L).orElseThrow();

        // Then
        assertThat(inventory.getCreatedAt()).isNotNull();
        assertThat(inventory.getUpdatedAt()).isNotNull();
    }
}
