package br.com.fiap.phase4.exec.adapter.in.kafka;

import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.exec.domain.port.in.StartExecutionUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExecKafkaConsumerTest {

    @Mock
    private StartExecutionUseCase startExecutionUseCase;

    @InjectMocks
    private ExecKafkaConsumer consumer;

    @Test
    @DisplayName("Should process PaymentConfirmedEvent and enqueue execution order")
    void shouldProcessPaymentConfirmedEvent() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "workOrderId", workOrderId.toString(),
                "invoiceId", UUID.randomUUID().toString(),
                "amount", 250.0
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "PaymentConfirmedEvent",
                "15soat-phase4-api-billing",
                "trace-123",
                payload
        );

        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                "payment-events",
                0,
                0L,
                workOrderId.toString(),
                envelope
        );

        consumer.consumePaymentEvents(record);

        verify(startExecutionUseCase).startExecution(eq(workOrderId), eq(null), anyString());
    }

    @Test
    @DisplayName("Should ignore unrecognized event type")
    void shouldIgnoreUnrecognizedEvent() {
        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "SomeOtherEvent",
                "15soat-phase4-api-billing",
                "trace-123",
                Map.of()
        );

        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                "payment-events",
                0,
                0L,
                "key",
                envelope
        );

        consumer.consumePaymentEvents(record);

        verify(startExecutionUseCase, never()).startExecution(any(), any(), any());
    }
}
