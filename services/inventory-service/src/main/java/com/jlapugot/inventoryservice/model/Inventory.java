package com.jlapugot.inventoryservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Inventory entity representing product stock levels
 */
@Entity
@Table(name = "inventory",
        indexes = {
                @Index(name = "idx_product_id", columnList = "productId"),
                @Index(name = "idx_updated_at", columnList = "updatedAt")
        })
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantityAvailable;

    @Column(nullable = false)
    private Integer quantityReserved;

    @Column(nullable = false)
    private Integer reorderLevel;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if there is sufficient stock available
     */
    public boolean hasSufficientStock(int quantity) {
        return quantityAvailable >= quantity;
    }

    /**
     * Reserve quantity for an order
     */
    public void reserveQuantity(int quantity) {
        if (!hasSufficientStock(quantity)) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %d. Available: %d, Requested: %d",
                            productId, quantityAvailable, quantity));
        }
        this.quantityAvailable -= quantity;
        this.quantityReserved += quantity;
    }

    /**
     * Release reserved quantity (e.g., order cancelled)
     */
    public void releaseReservedQuantity(int quantity) {
        if (quantityReserved < quantity) {
            throw new IllegalStateException(
                    String.format("Cannot release %d units. Only %d units are reserved",
                            quantity, quantityReserved));
        }
        this.quantityReserved -= quantity;
        this.quantityAvailable += quantity;
    }

    /**
     * Confirm reserved quantity (e.g., order shipped)
     */
    public void confirmReservedQuantity(int quantity) {
        if (quantityReserved < quantity) {
            throw new IllegalStateException(
                    String.format("Cannot confirm %d units. Only %d units are reserved",
                            quantity, quantityReserved));
        }
        this.quantityReserved -= quantity;
    }

    /**
     * Check if reorder is needed
     */
    public boolean needsReorder() {
        return (quantityAvailable + quantityReserved) <= reorderLevel;
    }
}
