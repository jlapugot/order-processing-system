package com.jlapugot.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application class for Order Service.
 * Enables caching and Kafka support.
 * JPA auditing is configured in JpaConfig.
 */
@SpringBootApplication(scanBasePackages = {
        "com.jlapugot.orderservice",
        "com.jlapugot.common"
})
@EnableCaching
@EnableKafka
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
