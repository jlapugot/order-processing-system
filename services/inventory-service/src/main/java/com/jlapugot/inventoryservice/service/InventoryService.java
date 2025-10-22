package com.jlapugot.inventoryservice.service;

import com.jlapugot.common.exceptions.BusinessException;
import com.jlapugot.common.exceptions.ResourceNotFoundException;
import com.jlapugot.inventoryservice.model.Inventory;
import com.jlapugot.inventoryservice.repository.InventoryRepository;
import com.jlapugot.common.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing inventory operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    // Track order-to-product mappings for reservation management
    private final Map<Long, ReservationInfo> orderReservations = new ConcurrentHashMap<>();

    /**
     * Reserve inventory for an order
     */
    public void reserveInventory(Long productId, Integer quantity, Long orderId) {
        log.info("Reserving {} units of product {} for order {}", quantity, productId, orderId);

        // Check if already reserved (idempotency)
        if (orderReservations.containsKey(orderId)) {
            log.warn("Order {} already has inventory reserved. Skipping duplicate reservation.", orderId);
            return;
        }

        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

        if (!inventory.hasSufficientStock(quantity)) {
            log.error("Insufficient stock for product {}. Available: {}, Requested: {}",
                    productId, inventory.getQuantityAvailable(), quantity);
            throw new BusinessException(
                    String.format("Insufficient stock for product %d", productId),
                    "INSUFFICIENT_STOCK"
            );
        }

        inventory.reserveQuantity(quantity);
        inventoryRepository.save(inventory);

        // Track the reservation
        orderReservations.put(orderId, new ReservationInfo(productId, quantity));

        log.info("Successfully reserved {} units of product {} for order {}",
                quantity, productId, orderId);

        // Check if reorder is needed
        if (inventory.needsReorder()) {
            log.warn("Product {} needs reordering. Current stock: {}, Reorder level: {}",
                    productId, inventory.getQuantityAvailable() + inventory.getQuantityReserved(),
                    inventory.getReorderLevel());
        }
    }

    /**
     * Release reserved inventory (e.g., order cancelled)
     */
    public void releaseReservedInventory(Long orderId) {
        log.info("Releasing reserved inventory for order {}", orderId);

        ReservationInfo reservation = orderReservations.get(orderId);
        if (reservation == null) {
            log.warn("No reservation found for order {}. Skipping release.", orderId);
            return;
        }

        Inventory inventory = inventoryRepository.findByProductIdWithLock(reservation.productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", reservation.productId));

        inventory.releaseReservedQuantity(reservation.quantity);
        inventoryRepository.save(inventory);

        orderReservations.remove(orderId);

        log.info("Successfully released {} units of product {} for order {}",
                reservation.quantity, reservation.productId, orderId);
    }

    /**
     * Confirm reserved inventory (e.g., order shipped)
     */
    public void confirmReservedInventory(Long orderId) {
        log.info("Confirming reserved inventory for order {}", orderId);

        ReservationInfo reservation = orderReservations.get(orderId);
        if (reservation == null) {
            log.warn("No reservation found for order {}. Skipping confirmation.", orderId);
            return;
        }

        Inventory inventory = inventoryRepository.findByProductIdWithLock(reservation.productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", reservation.productId));

        inventory.confirmReservedQuantity(reservation.quantity);
        inventoryRepository.save(inventory);

        orderReservations.remove(orderId);

        log.info("Successfully confirmed {} units of product {} for order {}",
                reservation.quantity, reservation.productId, orderId);
    }

    /**
     * Handle order status changes
     */
    public void handleOrderStatusChange(Long orderId, OrderStatus previousStatus, OrderStatus newStatus) {
        log.info("Handling order status change for order {}: {} -> {}",
                orderId, previousStatus, newStatus);

        // Confirm inventory when order is shipped
        if (newStatus == OrderStatus.SHIPPED) {
            confirmReservedInventory(orderId);
        }
        // Release inventory if order fails
        else if (newStatus == OrderStatus.FAILED) {
            releaseReservedInventory(orderId);
        }
    }

    /**
     * Inner class to track reservation information
     */
    private record ReservationInfo(Long productId, Integer quantity) {
    }
}
