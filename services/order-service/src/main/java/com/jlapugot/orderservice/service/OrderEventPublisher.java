package com.jlapugot.orderservice.service;

import com.jlapugot.common.utils.CorrelationIdUtils;
import com.jlapugot.common.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing order events to Kafka
 * Handles async event publishing with proper error handling and logging
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    /**
     * Publishes an event to the specified Kafka topic asynchronously
     *
     * @param topic the Kafka topic name
     * @param key the message key (typically order ID)
     * @param event the order event to publish
     */
    @Async
    public void publishEvent(String topic, String key, OrderEvent event) {
        String correlationId = CorrelationIdUtils.getOrGenerate();
        log.debug("Publishing event to topic '{}' with key '{}' and correlationId '{}'",
                topic, key, correlationId);

        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published event to topic '{}' with key '{}', partition={}, offset={}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event to topic '{}' with key '{}': {}",
                        topic, key, ex.getMessage(), ex);
            }
        });
    }

    /**
     * Publishes an event synchronously and waits for confirmation
     * Use this when you need to ensure the event was published before proceeding
     *
     * @param topic the Kafka topic name
     * @param key the message key (typically order ID)
     * @param event the order event to publish
     * @throws Exception if publishing fails
     */
    public void publishEventSync(String topic, String key, OrderEvent event) throws Exception {
        String correlationId = CorrelationIdUtils.getOrGenerate();
        log.debug("Publishing event synchronously to topic '{}' with key '{}' and correlationId '{}'",
                topic, key, correlationId);

        SendResult<String, OrderEvent> result = kafkaTemplate.send(topic, key, event).get();

        log.info("Successfully published event synchronously to topic '{}' with key '{}', partition={}, offset={}",
                topic, key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }
}
