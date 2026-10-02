package br.com.fiap.phase4.exec.adapter.out.kafka;

import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.commons.kafka.tracing.KafkaTraceContextUtils;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.port.out.ExecutionEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecKafkaPublisherAdapter implements ExecutionEventPublisherPort {

    public static final String EXECUTION_EVENTS_TOPIC = "execution-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishExecutionStarted(ExecutionOrder order) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "executionId", order.getId().toString(),
                "workOrderId", order.getWorkOrderId().toString(),
                "technicianId", order.getTechnicianId() != null ? order.getTechnicianId() : "",
                "status", order.getStatus().name()
        );

        var envelope = EventEnvelope.of(
                "ExecutionStartedEvent",
                "api-exec",
                traceId,
                payload
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                EXECUTION_EVENTS_TOPIC,
                order.getWorkOrderId().toString(),
                envelope
        );

        KafkaTraceContextUtils.injectTraceContext(record.headers(), traceId, null);
        kafkaTemplate.send(record);
        log.info("Published ExecutionStartedEvent for Work Order ID: {}", order.getWorkOrderId());
    }

    @Override
    public void publishExecutionCompleted(ExecutionOrder order) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "executionId", order.getId().toString(),
                "workOrderId", order.getWorkOrderId().toString(),
                "status", order.getStatus().name(),
                "completedAt", order.getCompletedAt() != null ? order.getCompletedAt().toString() : ""
        );

        var envelope = EventEnvelope.of(
                "ExecutionCompletedEvent",
                "api-exec",
                traceId,
                payload
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                EXECUTION_EVENTS_TOPIC,
                order.getWorkOrderId().toString(),
                envelope
        );

        KafkaTraceContextUtils.injectTraceContext(record.headers(), traceId, null);
        kafkaTemplate.send(record);
        log.info("Published ExecutionCompletedEvent for Work Order ID: {}", order.getWorkOrderId());
    }

    @Override
    public void publishExecutionFailed(ExecutionOrder order, String reason) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "executionId", order.getId().toString(),
                "workOrderId", order.getWorkOrderId().toString(),
                "status", order.getStatus().name(),
                "reason", reason != null ? reason : "Repair failed"
        );

        var envelope = EventEnvelope.of(
                "ExecutionFailedEvent",
                "api-exec",
                traceId,
                payload
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                EXECUTION_EVENTS_TOPIC,
                order.getWorkOrderId().toString(),
                envelope
        );

        KafkaTraceContextUtils.injectTraceContext(record.headers(), traceId, null);
        kafkaTemplate.send(record);
        log.warn("Published ExecutionFailedEvent (Saga Compensation Trigger) for Work Order ID: {}", order.getWorkOrderId());
    }
}
