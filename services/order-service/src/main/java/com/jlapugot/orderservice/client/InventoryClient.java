package com.jlapugot.orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for communicating with Inventory Service
 * Implements circuit breaker pattern for resilience
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final InventoryApiClient inventoryApiClient;

    /**
     * Check if inventory is available for a product
     * Uses circuit breaker to handle failures gracefully
     *
     * @param productId the product ID
     * @param quantity the quantity to check
     * @return true if inventory is available, false otherwise
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "checkInventoryFallback")
    public boolean checkInventoryAvailability(Long productId, int quantity) {
        log.debug("Checking inventory availability for product: {} quantity: {}", productId, quantity);
        return inventoryApiClient.checkInventory(productId, quantity);
    }

    /**
     * Fallback method for inventory check
     * Returns false (conservative approach) when service is unavailable
     *
     * @param productId the product ID
     * @param quantity the quantity
     * @param throwable the exception that triggered the fallback
     * @return false (conservative fallback)
     */
    private boolean checkInventoryFallback(Long productId, int quantity, Throwable throwable) {
        log.warn("Inventory service unavailable, using fallback. Product: {}, Quantity: {}, Error: {}",
                productId, quantity, throwable.getMessage());
        return false; // Conservative approach: assume inventory not available
    }

    /**
     * Internal interface for the actual API call
     * This allows for easy mocking in tests
     */
    @Component
    @Slf4j
    public static class InventoryApiClient {

        private final RestTemplate restTemplate;
        private final String inventoryServiceUrl;

        public InventoryApiClient(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
            this.inventoryServiceUrl = "http://inventory-service:8081"; // Default, should be configurable
        }

        /**
         * Make actual HTTP call to inventory service
         *
         * @param productId the product ID
         * @param quantity the quantity
         * @return true if inventory is available
         */
        public boolean checkInventory(Long productId, int quantity) {
            String url = String.format("%s/api/inventory/check?productId=%d&quantity=%d",
                    inventoryServiceUrl, productId, quantity);

            log.debug("Calling inventory service: {}", url);

            try {
                Boolean result = restTemplate.getForObject(url, Boolean.class);
                return result != null && result;
            } catch (Exception e) {
                log.error("Failed to check inventory: {}", e.getMessage());
                throw new RuntimeException("Inventory service unavailable", e);
            }
        }
    }
}
