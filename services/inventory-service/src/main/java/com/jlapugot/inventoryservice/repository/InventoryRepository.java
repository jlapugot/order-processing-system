package com.jlapugot.inventoryservice.repository;

import com.jlapugot.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Inventory entity
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Find inventory by product ID with pessimistic write lock
     * to prevent concurrent modifications
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);

    /**
     * Find inventory by product ID
     */
    Optional<Inventory> findByProductId(Long productId);

    /**
     * Find all products that need reordering
     */
    @Query("SELECT i FROM Inventory i WHERE (i.quantityAvailable + i.quantityReserved) <= i.reorderLevel")
    List<Inventory> findProductsNeedingReorder();

    /**
     * Check if product exists
     */
    boolean existsByProductId(Long productId);
}
