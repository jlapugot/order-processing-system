package com.jlapugot.inventoryservice.consumer;

import com.jlapugot.common.events.EventType;
import com.jlapugot.common.utils.CorrelationIdUtils;
import com.jlapugot.inventoryservice.service.InventoryService;
import com.jlapugot.common.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events
 * Listens to order events and updates inventory accordingly
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final InventoryService inventoryService;

    /**
     * Consume order created events
     */
    @KafkaListener(
            topics = "order.created",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            // Set correlation ID for distributed tracing
            CorrelationIdUtils.set(event.getCorrelationId());

            log.info("Received order.created event - orderId: {}, productId: {}, quantity: {}, partition: {}, offset: {}",
                    event.getOrderId(), event.getProductId(), event.getQuantity(), partition, offset);

            // Reserve inventory
            inventoryService.reserveInventory(event.getProductId(), event.getQuantity(), event.getOrderId());

            // Manually commit offset after successful processing
            acknowledgment.acknowledge();

            log.info("Successfully processed order.created event for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order.created event for orderId: {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            // Don't acknowledge - message will be retried or sent to DLQ
            throw e;
        } finally {
            CorrelationIdUtils.clear();
        }
    }

    /**
     * Consume order cancelled events
     */
    @KafkaListener(
            topics = "order.cancelled",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCancelled(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            CorrelationIdUtils.set(event.getCorrelationId());

            log.info("Received order.cancelled event - orderId: {}, partition: {}, offset: {}",
                    event.getOrderId(), partition, offset);

            // Release reserved inventory
            inventoryService.releaseReservedInventory(event.getOrderId());

            acknowledgment.acknowledge();

            log.info("Successfully processed order.cancelled event for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order.cancelled event for orderId: {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            CorrelationIdUtils.clear();
        }
    }

    /**
     * Consume order updated events
     */
    @KafkaListener(
            topics = "order.updated",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderUpdated(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            CorrelationIdUtils.set(event.getCorrelationId());

            log.info("Received order.updated event - orderId: {}, previousStatus: {}, newStatus: {}, partition: {}, offset: {}",
                    event.getOrderId(), event.getPreviousStatus(), event.getStatus(), partition, offset);

            // Handle status transitions that affect inventory
            inventoryService.handleOrderStatusChange(event.getOrderId(), event.getPreviousStatus(), event.getStatus());

            acknowledgment.acknowledge();

            log.info("Successfully processed order.updated event for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order.updated event for orderId: {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            CorrelationIdUtils.clear();
        }
    }
}
