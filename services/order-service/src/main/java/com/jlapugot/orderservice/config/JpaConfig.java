package com.jlapugot.orderservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration
 * Separated from main application class to allow easier testing
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
