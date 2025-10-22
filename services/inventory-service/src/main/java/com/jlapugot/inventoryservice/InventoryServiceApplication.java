package com.jlapugot.inventoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application class for Inventory Service.
 * Listens to order events and manages inventory accordingly.
 */
@SpringBootApplication(scanBasePackages = {
        "com.jlapugot.inventoryservice",
        "com.jlapugot.common"
})
@EnableKafka
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
