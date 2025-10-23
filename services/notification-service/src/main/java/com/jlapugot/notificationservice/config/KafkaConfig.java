package com.jlapugot.notificationservice.config;

import com.jlapugot.common.events.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration
 * Includes DLQ (Dead Letter Queue) and retry configuration
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.retry.max-attempts:3}")
    private long maxRetryAttempts;

    @Value("${spring.kafka.consumer.retry.backoff-interval:2000}")
    private long backoffInterval;

    @Bean
    public ConsumerFactory<String, OrderEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderEvent.class.getName());
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.jlapugot.common.events");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        // Session and heartbeat settings
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ProducerFactory<String, OrderEvent> dlqProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, OrderEvent> dlqKafkaTemplate) {
        // Configure retry with fixed backoff
        FixedBackOff fixedBackOff = new FixedBackOff(backoffInterval, maxRetryAttempts);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler((consumerRecord, exception) -> {
            // Send to DLQ after max retries
            log.error("Message processing failed after {} retries. Sending to DLQ. Topic: {}, Partition: {}, Offset: {}, Error: {}",
                    maxRetryAttempts, consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset(), exception.getMessage());

            String dlqTopic = consumerRecord.topic() + ".dlq";
            try {
                String key = consumerRecord.key() != null ? consumerRecord.key().toString() : null;
                dlqKafkaTemplate.send(dlqTopic, key, (OrderEvent) consumerRecord.value());
                log.info("Successfully sent message to DLQ topic: {}", dlqTopic);
            } catch (Exception e) {
                log.error("Failed to send message to DLQ topic: {}", dlqTopic, e);
            }
        }, fixedBackOff);

        // Don't retry for these exceptions
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class
        );

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> kafkaListenerContainerFactory(
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3); // Number of concurrent consumers

        // Set error handler with retry and DLQ
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
