package com.jlapugot.inventoryservice.consumer;

import com.jlapugot.common.events.OrderEvent;
import com.jlapugot.inventoryservice.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Integration test for Order Event Consumer with DLQ and retry functionality
 */
@SpringBootTest(
        properties = "spring.profiles.active=test"
)
@EmbeddedKafka(
        partitions = 1,
        topics = {"order.created", "order.created.dlq"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
class OrderEventConsumerIntegrationTest {

    private static final String TOPIC = "order.created";
    private static final String DLQ_TOPIC = "order.created.dlq";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockBean
    private InventoryService inventoryService;

    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private Consumer<String, OrderEvent> dlqConsumer;

    @BeforeEach
    void setUp() {
        // Setup producer
        Map<String, Object> producerProps = new HashMap<>(
                KafkaTestUtils.producerProps(embeddedKafkaBroker)
        );
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, OrderEvent> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);

        // Setup DLQ consumer
        Map<String, Object> consumerProps = new HashMap<>(
                KafkaTestUtils.consumerProps("dlq-test-group", "true", embeddedKafkaBroker)
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderEvent.class.getName());
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, OrderEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        dlqConsumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(dlqConsumer, DLQ_TOPIC);
    }

    @Test
    void shouldProcessMessageSuccessfully() {
        // Given
        OrderEvent event = OrderEvent.created(
                123L, 456L, "John Doe", "john@example.com",
                1L, "Product A", 5, BigDecimal.TEN, BigDecimal.valueOf(50),
                "123 Main St", "test-correlation-id"
        );

        doNothing().when(inventoryService).reserveInventory(anyLong(), anyInt(), anyLong());

        // When
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);

        // Then
        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(inventoryService, times(1))
                        .reserveInventory(1L, 5, 123L));

        log.info("Message processed successfully");
    }

    @Test
    void shouldRetryAndSendToDLQAfterMaxAttempts() {
        // Given
        OrderEvent event = OrderEvent.created(
                789L, 999L, "Jane Doe", "jane@example.com",
                2L, "Product B", 10, BigDecimal.valueOf(20), BigDecimal.valueOf(200),
                "456 Oak Ave", "test-correlation-id-2"
        );

        // Simulate failure on all attempts
        doThrow(new RuntimeException("Simulated processing error"))
                .when(inventoryService).reserveInventory(anyLong(), anyInt(), anyLong());

        // When
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);

        // Then - verify retries (should be called 3 times total: 1 initial + 2 retries, as per max-attempts=3)
        await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> verify(inventoryService, times(3))
                        .reserveInventory(2L, 10, 789L));

        // Verify message sent to DLQ
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    ConsumerRecord<String, OrderEvent> dlqRecord =
                            KafkaTestUtils.getSingleRecord(dlqConsumer, DLQ_TOPIC, Duration.ofSeconds(5));

                    assertThat(dlqRecord).isNotNull();
                    assertThat(dlqRecord.value().getOrderId()).isEqualTo(789L);
                    assertThat(dlqRecord.value().getCustomerId()).isEqualTo(999L);
                    log.info("Message successfully sent to DLQ after max retries");
                });
    }

    @Test
    void shouldNotRetryForNonRetryableExceptions() {
        // Given
        OrderEvent event = OrderEvent.created(
                456L, 789L, "Bob Smith", "bob@example.com",
                3L, "Product C", 3, BigDecimal.valueOf(30), BigDecimal.valueOf(90),
                "789 Elm St", "test-correlation-id-3"
        );

        // Simulate non-retryable exception
        doThrow(new IllegalArgumentException("Invalid order data"))
                .when(inventoryService).reserveInventory(anyLong(), anyInt(), anyLong());

        // When
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);

        // Then - should only be called once (no retries for IllegalArgumentException)
        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(inventoryService, times(1))
                        .reserveInventory(3L, 3, 456L));

        // Verify message sent directly to DLQ without retries
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    ConsumerRecord<String, OrderEvent> dlqRecord =
                            KafkaTestUtils.getSingleRecord(dlqConsumer, DLQ_TOPIC, Duration.ofSeconds(5));

                    assertThat(dlqRecord).isNotNull();
                    assertThat(dlqRecord.value().getOrderId()).isEqualTo(456L);
                    log.info("Non-retryable exception sent to DLQ without retries");
                });
    }

    @Test
    void shouldSucceedAfterTransientFailure() {
        // Given
        OrderEvent event = OrderEvent.created(
                111L, 222L, "Alice Johnson", "alice@example.com",
                4L, "Product D", 7, BigDecimal.valueOf(15), BigDecimal.valueOf(105),
                "321 Pine St", "test-correlation-id-4"
        );

        // Simulate transient failure (fail once, then succeed on first retry)
        doThrow(new RuntimeException("Transient error"))
                .doNothing()
                .when(inventoryService).reserveInventory(anyLong(), anyInt(), anyLong());

        // When
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);

        // Then - should be called 2 times (1 initial failure + 1 successful retry)
        await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> verify(inventoryService, times(2))
                        .reserveInventory(4L, 7, 111L));

        // Verify no message sent to DLQ (processing succeeded on retry)
        // Use poll() instead of getSingleRecord() to avoid exception when no records exist
        await()
                .pollDelay(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    org.apache.kafka.clients.consumer.ConsumerRecords<String, OrderEvent> records =
                            dlqConsumer.poll(Duration.ofMillis(500));
                    assertThat(records.isEmpty())
                            .as("No message should be sent to DLQ when processing succeeds")
                            .isTrue();
                    log.info("Message processed successfully after transient failures - no DLQ message as expected");
                });
    }
}
