package br.com.fiap.phase4.exec.adapter.out.kafka;

import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExecKafkaPublisherAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ExecKafkaPublisherAdapter publisherAdapter;

    private UUID executionId;
    private UUID workOrderId;
    private ExecutionOrder sampleOrder;

    @BeforeEach
    void setUp() {
        executionId = UUID.randomUUID();
        workOrderId = UUID.randomUUID();
        sampleOrder = new ExecutionOrder(
                executionId,
                workOrderId,
                "TECH-101",
                ExecutionStatus.IN_REPAIR,
                "Repair notes",
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should publish ExecutionStartedEvent to execution-events topic")
    void shouldPublishExecutionStarted() {
        publisherAdapter.publishExecutionStarted(sampleOrder);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, Object> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(ExecKafkaPublisherAdapter.EXECUTION_EVENTS_TOPIC);
        assertThat(record.key()).isEqualTo(workOrderId.toString());

        EventEnvelope<Map<String, Object>> envelope = (EventEnvelope<Map<String, Object>>) record.value();
        assertThat(envelope.metadata().eventType()).isEqualTo("ExecutionStartedEvent");
        assertThat(envelope.payload().get("executionId")).isEqualTo(executionId.toString());
        assertThat(envelope.payload().get("workOrderId")).isEqualTo(workOrderId.toString());
    }

    @Test
    @DisplayName("Should publish ExecutionCompletedEvent to execution-events topic")
    void shouldPublishExecutionCompleted() {
        publisherAdapter.publishExecutionCompleted(sampleOrder);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, Object> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(ExecKafkaPublisherAdapter.EXECUTION_EVENTS_TOPIC);

        EventEnvelope<Map<String, Object>> envelope = (EventEnvelope<Map<String, Object>>) record.value();
        assertThat(envelope.metadata().eventType()).isEqualTo("ExecutionCompletedEvent");
        assertThat(envelope.payload().get("workOrderId")).isEqualTo(workOrderId.toString());
    }

    @Test
    @DisplayName("Should publish ExecutionFailedEvent to execution-events topic")
    void shouldPublishExecutionFailed() {
        publisherAdapter.publishExecutionFailed(sampleOrder, "Parts unavailable");

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, Object> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(ExecKafkaPublisherAdapter.EXECUTION_EVENTS_TOPIC);

        EventEnvelope<Map<String, Object>> envelope = (EventEnvelope<Map<String, Object>>) record.value();
        assertThat(envelope.metadata().eventType()).isEqualTo("ExecutionFailedEvent");
        assertThat(envelope.payload().get("reason")).isEqualTo("Parts unavailable");
    }
}
