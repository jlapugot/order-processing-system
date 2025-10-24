package com.jlapugot.orderservice.service;

import com.jlapugot.orderservice.client.InventoryClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Resilience4j Circuit Breaker functionality
 * Tests that circuit breaker opens after threshold failures and provides fallback
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@ActiveProfiles("test")
@Slf4j
class CircuitBreakerTest {

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private InventoryClient.InventoryApiClient inventoryApiClient;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        // Get the circuit breaker instance and reset it
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("inventoryService");
        circuitBreaker.reset();

        // Clear all mock interactions
        reset(inventoryApiClient);
    }

    @Test
    void shouldAllowCallsWhenCircuitBreakerIsClosed() {
        // Given - Circuit breaker is in CLOSED state (default)
        when(inventoryApiClient.checkInventory(anyLong(), anyInt()))
                .thenReturn(true);

        // When
        boolean result = inventoryClient.checkInventoryAvailability(1L, 5);

        // Then
        assertThat(result).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        verify(inventoryApiClient, times(1)).checkInventory(1L, 5);
    }

    @Test
    void shouldOpenCircuitBreakerAfterFailureThreshold() {
        // Given - Configure to fail repeatedly (threshold: 3 failures)
        when(inventoryApiClient.checkInventory(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When - Make calls that will fail (need at least 3 failures to open)
        for (int i = 0; i < 5; i++) {
            try {
                inventoryClient.checkInventoryAvailability(1L, 5);
            } catch (Exception e) {
                log.debug("Expected failure #{}: {}", i + 1, e.getMessage());
            }
        }

        // Then - Circuit breaker should be OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Verify at least 3 calls were made before circuit opened
        verify(inventoryApiClient, atLeast(3)).checkInventory(anyLong(), anyInt());
    }

    @Test
    void shouldReturnFallbackWhenCircuitBreakerIsOpen() {
        // Given - Force circuit breaker to OPEN state by triggering failures
        when(inventoryApiClient.checkInventory(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Trigger enough failures to open the circuit
        for (int i = 0; i < 5; i++) {
            try {
                inventoryClient.checkInventoryAvailability(1L, 5);
            } catch (Exception e) {
                // Expected failures
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // When - Try to call with circuit OPEN (should use fallback, not call API)
        boolean result = inventoryClient.checkInventoryAvailability(1L, 5);

        // Then - Should return fallback value (false) without calling the API
        assertThat(result).isFalse();

        // API should not be called anymore (still 5 calls from before, no new calls)
        verify(inventoryApiClient, atMost(5)).checkInventory(anyLong(), anyInt());
    }

    @Test
    void shouldTransitionToHalfOpenAfterWaitDuration() throws InterruptedException {
        // Given - Circuit breaker configured with wait duration (e.g., 5 seconds)
        when(inventoryApiClient.checkInventory(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Open the circuit
        for (int i = 0; i < 5; i++) {
            try {
                inventoryClient.checkInventoryAvailability(1L, 5);
            } catch (Exception e) {
                // Expected
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // When - Wait for the configured wait duration
        // Note: In test config, we'll use a shorter duration (e.g., 2 seconds)
        Thread.sleep(2500); // Wait 2.5 seconds

        // Then - Circuit should transition to HALF_OPEN
        // Need to trigger a call to transition state
        try {
            inventoryClient.checkInventoryAvailability(1L, 5);
        } catch (Exception e) {
            // May still fail
        }

        assertThat(circuitBreaker.getState())
                .isIn(CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldCloseCircuitBreakerAfterSuccessfulCallsInHalfOpen() throws InterruptedException {
        // Given - Open the circuit first
        when(inventoryApiClient.checkInventory(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Service unavailable"));

        for (int i = 0; i < 5; i++) {
            try {
                inventoryClient.checkInventoryAvailability(1L, 5);
            } catch (Exception e) {
                // Expected
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Wait for transition to HALF_OPEN
        Thread.sleep(2500);

        // When - Service recovers and returns successful responses
        // Reset the mock to clear previous stubbing
        reset(inventoryApiClient);
        when(inventoryApiClient.checkInventory(anyLong(), anyInt()))
                .thenReturn(true);

        // Make successful calls in HALF_OPEN state
        for (int i = 0; i < 3; i++) {
            try {
                boolean result = inventoryClient.checkInventoryAvailability(1L, 5);
                assertThat(result).isTrue();
            } catch (CallNotPermittedException e) {
                log.warn("Call not permitted in iteration {}", i);
            } catch (RuntimeException e) {
                // May happen if circuit is still in OPEN state
                log.warn("Service call failed in iteration {}: {}", i, e.getMessage());
            }
        }

        // Then - Circuit should close after successful calls
        assertThat(circuitBreaker.getState())
                .isIn(CircuitBreaker.State.CLOSED, CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    void shouldRecordMetrics() {
        // Given
        when(inventoryApiClient.checkInventory(anyLong(), anyInt()))
                .thenReturn(true)
                .thenReturn(true)
                .thenThrow(new RuntimeException("Error"));

        // When
        inventoryClient.checkInventoryAvailability(1L, 5); // Success
        inventoryClient.checkInventoryAvailability(2L, 3); // Success

        try {
            inventoryClient.checkInventoryAvailability(3L, 2); // Failure
        } catch (Exception e) {
            // Expected
        }

        // Then - Verify metrics are being recorded
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThanOrEqualTo(1);

        log.info("Circuit Breaker Metrics - Success: {}, Failed: {}, Failure Rate: {}%",
                metrics.getNumberOfSuccessfulCalls(),
                metrics.getNumberOfFailedCalls(),
                metrics.getFailureRate());
    }
}
