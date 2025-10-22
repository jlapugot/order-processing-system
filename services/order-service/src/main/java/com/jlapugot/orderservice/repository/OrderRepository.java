package com.jlapugot.orderservice.repository;

import com.jlapugot.orderservice.model.Order;
import com.jlapugot.common.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Order entity.
 * Provides database operations with custom query methods for complex queries.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders for a specific customer
     *
     * @param customerId the customer ID
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Find all orders with a specific status
     *
     * @param status the order status
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find all orders for a customer with a specific status
     *
     * @param customerId the customer ID
     * @param status the order status
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

    /**
     * Find orders created within a date range
     *
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find orders by product ID
     *
     * @param productId the product ID
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByProductId(Long productId, Pageable pageable);

    /**
     * Count orders by status
     *
     * @param status the order status
     * @return count of orders
     */
    long countByStatus(OrderStatus status);

    /**
     * Find all orders for a customer ordered by creation date descending
     *
     * @param customerId the customer ID
     * @return list of orders
     */
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /**
     * Check if an order exists for a customer
     *
     * @param customerId the customer ID
     * @param orderId the order ID
     * @return true if exists, false otherwise
     */
    boolean existsByIdAndCustomerId(Long orderId, Long customerId);

    /**
     * Find the most recent order for a customer
     *
     * @param customerId the customer ID
     * @return optional containing the most recent order
     */
    Optional<Order> findFirstByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /**
     * Find orders that need processing (custom query)
     * Orders in PENDING or CONFIRMED status older than specified minutes
     *
     * @param statuses list of statuses to search
     * @param cutoffTime the cutoff time
     * @return list of orders needing processing
     */
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses AND o.createdAt < :cutoffTime")
    List<Order> findOrdersNeedingProcessing(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );

    /**
     * Find orders by customer email
     *
     * @param email customer email
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByCustomerEmailContainingIgnoreCase(String email, Pageable pageable);
}
