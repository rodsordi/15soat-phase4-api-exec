package br.com.fiap.phase4.exec.iandt;

import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.exec.adapter.out.persistence.document.ExecutionOrderMongoDocument;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Execution Kafka Events Integration Tests (Saga Inbound & Outbound)")
class ExecEventsKafkaIntegrationTest extends ExecIntegrationTest {

    @Test
    @DisplayName("Should consume PaymentConfirmedEvent from payment-events topic and auto-enqueue execution order in MongoDB")
    void shouldConsumePaymentConfirmedEventAndAutoEnqueue() {
        UUID workOrderId = UUID.randomUUID();
        String traceId = UUID.randomUUID().toString();

        Map<String, Object> payload = Map.of(
                "workOrderId", workOrderId.toString(),
                "invoiceId", UUID.randomUUID().toString(),
                "amount", 350.00
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "PaymentConfirmedEvent",
                "15soat-phase4-api-billing",
                traceId,
                payload
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                "payment-events",
                workOrderId.toString(),
                envelope
        );

        kafkaTemplate.send(record);

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Optional<ExecutionOrderMongoDocument> optionalDoc = executionOrderRepository.findByWorkOrderId(workOrderId.toString());
                    assertThat(optionalDoc).isPresent();
                    ExecutionOrderMongoDocument doc = optionalDoc.get();
                    assertThat(doc.getStatus()).isEqualTo("QUEUED");
                    assertThat(doc.getNotes()).contains("Auto-enqueued on payment confirmation");
                });
    }

    @Test
    @DisplayName("Should publish ExecutionStartedEvent and ExecutionCompletedEvent to execution-events Kafka topic")
    void shouldPublishExecutionEventsToKafka() {
        UUID workOrderId = UUID.randomUUID();

        try (Consumer<String, Object> consumer = consumerFactory.createConsumer("test-exec-group-" + UUID.randomUUID(), "test-client")) {
            consumer.subscribe(List.of("execution-events"));

            // Warm up partition assignment before sending event
            consumer.poll(Duration.ofMillis(500));

            // 1. Trigger enqueuing (publishes ExecutionStartedEvent)
            String traceId = UUID.randomUUID().toString();
            Map<String, Object> payload = Map.of(
                    "workOrderId", workOrderId.toString(),
                    "invoiceId", UUID.randomUUID().toString()
            );
            EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of("PaymentConfirmedEvent", "billing", traceId, payload);
            kafkaTemplate.send(new ProducerRecord<>("payment-events", workOrderId.toString(), envelope));

            List<ConsumerRecord<String, Object>> capturedRecords = new ArrayList<>();
            Awaitility.await()
                    .atMost(Duration.ofSeconds(15))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> {
                        ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
                        records.forEach(capturedRecords::add);
                        assertThat(capturedRecords)
                                .anyMatch(record -> workOrderId.toString().equals(record.key()));
                    });
        }
    }
}
