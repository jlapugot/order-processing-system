package com.jlapugot.orderservice.repository;

import com.jlapugot.orderservice.model.Order;
import com.jlapugot.common.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Order Repository Tests")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        order1 = Order.builder()
                .customerId(1L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .productId(100L)
                .productName("Product A")
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .status(OrderStatus.PENDING)
                .shippingAddress("123 Main St")
                .build();

        order2 = Order.builder()
                .customerId(2L)
                .customerName("Jane Smith")
                .customerEmail("jane@example.com")
                .productId(101L)
                .productName("Product B")
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .status(OrderStatus.CONFIRMED)
                .shippingAddress("456 Elm St")
                .build();

        orderRepository.saveAll(List.of(order1, order2));
    }

    @Test
    @DisplayName("Should save and retrieve order")
    void shouldSaveAndRetrieveOrder() {
        // Given
        Order newOrder = Order.builder()
                .customerId(3L)
                .customerName("Bob Wilson")
                .customerEmail("bob@example.com")
                .productId(102L)
                .productName("Product C")
                .quantity(3)
                .unitPrice(new BigDecimal("75.00"))
                .status(OrderStatus.PENDING)
                .shippingAddress("789 Oak St")
                .build();

        // When
        Order savedOrder = orderRepository.save(newOrder);

        // Then
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getCustomerName()).isEqualTo("Bob Wilson");
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("225.00"));
    }

    @Test
    @DisplayName("Should find orders by customer ID")
    void shouldFindOrdersByCustomerId() {
        // When
        Page<Order> orders = orderRepository.findByCustomerId(1L, PageRequest.of(0, 10));

        // Then
        assertThat(orders.getTotalElements()).isEqualTo(1);
        assertThat(orders.getContent().get(0).getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should find orders by status")
    void shouldFindOrdersByStatus() {
        // When
        Page<Order> orders = orderRepository.findByStatus(OrderStatus.PENDING, PageRequest.of(0, 10));

        // Then
        assertThat(orders.getTotalElements()).isEqualTo(1);
        assertThat(orders.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Should find orders by customer ID and status")
    void shouldFindOrdersByCustomerIdAndStatus() {
        // When
        Page<Order> orders = orderRepository.findByCustomerIdAndStatus(
                1L, OrderStatus.PENDING, PageRequest.of(0, 10));

        // Then
        assertThat(orders.getTotalElements()).isEqualTo(1);
        assertThat(orders.getContent().get(0).getCustomerId()).isEqualTo(1L);
        assertThat(orders.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Should count orders by status")
    void shouldCountOrdersByStatus() {
        // When
        long count = orderRepository.countByStatus(OrderStatus.PENDING);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should check if order exists by ID and customer ID")
    void shouldCheckIfOrderExistsByIdAndCustomerId() {
        // Given
        Long orderId = order1.getId();

        // When
        boolean exists = orderRepository.existsByIdAndCustomerId(orderId, 1L);
        boolean notExists = orderRepository.existsByIdAndCustomerId(orderId, 999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find first order by customer ID ordered by created date")
    void shouldFindFirstOrderByCustomerIdOrderedByCreatedDate() {
        // When
        Optional<Order> order = orderRepository.findFirstByCustomerIdOrderByCreatedAtDesc(1L);

        // Then
        assertThat(order).isPresent();
        assertThat(order.get().getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should find orders by email containing ignore case")
    void shouldFindOrdersByEmailContainingIgnoreCase() {
        // When
        Page<Order> orders = orderRepository.findByCustomerEmailContainingIgnoreCase(
                "JOHN", PageRequest.of(0, 10));

        // Then
        assertThat(orders.getTotalElements()).isEqualTo(1);
        assertThat(orders.getContent().get(0).getCustomerEmail()).contains("john");
    }

    @Test
    @DisplayName("Should find orders created between dates")
    void shouldFindOrdersCreatedBetweenDates() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // When
        Page<Order> orders = orderRepository.findByCreatedAtBetween(
                startDate, endDate, PageRequest.of(0, 10));

        // Then
        assertThat(orders.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find orders by product ID")
    void shouldFindOrdersByProductId() {
        // When
        Page<Order> orders = orderRepository.findByProductId(100L, PageRequest.of(0, 10));

        // Then
        assertThat(orders.getTotalElements()).isEqualTo(1);
        assertThat(orders.getContent().get(0).getProductId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should calculate total amount on save")
    void shouldCalculateTotalAmountOnSave() {
        // Given
        Order order = Order.builder()
                .customerId(4L)
                .customerName("Test User")
                .customerEmail("test@example.com")
                .productId(103L)
                .productName("Product D")
                .quantity(5)
                .unitPrice(new BigDecimal("20.00"))
                .status(OrderStatus.PENDING)
                .shippingAddress("Test Address")
                .build();

        // When
        Order savedOrder = orderRepository.save(order);

        // Then
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
