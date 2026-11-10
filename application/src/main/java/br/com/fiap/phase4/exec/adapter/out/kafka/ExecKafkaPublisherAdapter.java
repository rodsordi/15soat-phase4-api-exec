package br.com.fiap.phase4.exec.adapter.out.kafka;

import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.commons.kafka.tracing.KafkaTraceContextUtils;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.port.out.ExecutionEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecKafkaPublisherAdapter implements ExecutionEventPublisherPort {

    public static final String EXECUTION_EVENTS_TOPIC = "execution-events";
    private static final String SOURCE_SERVICE = "15soat-phase4-api-exec";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishExecutionStarted(ExecutionOrder order) {
        String traceId = getOrCreateTraceId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("executionId", order.getId() != null ? order.getId().toString() : null);
        payload.put("workOrderId", order.getWorkOrderId() != null ? order.getWorkOrderId().toString() : null);
        payload.put("technicianId", order.getTechnicianId());
        payload.put("timestamp", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);

        var envelope = EventEnvelope.of("ExecutionStartedEvent", SOURCE_SERVICE, traceId, payload);
        publish(order.getWorkOrderId(), envelope, traceId);
        log.info("Published ExecutionStartedEvent for OS ID: {} on topic: {}", order.getWorkOrderId(), EXECUTION_EVENTS_TOPIC);
    }

    @Override
    public void publishExecutionCompleted(ExecutionOrder order) {
        String traceId = getOrCreateTraceId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("executionId", order.getId() != null ? order.getId().toString() : null);
        payload.put("workOrderId", order.getWorkOrderId() != null ? order.getWorkOrderId().toString() : null);
        payload.put("timestamp", order.getCompletedAt() != null ? order.getCompletedAt().toString() : null);

        var envelope = EventEnvelope.of("ExecutionCompletedEvent", SOURCE_SERVICE, traceId, payload);
        publish(order.getWorkOrderId(), envelope, traceId);
        log.info("Published ExecutionCompletedEvent for OS ID: {} on topic: {}", order.getWorkOrderId(), EXECUTION_EVENTS_TOPIC);
    }

    @Override
    public void publishExecutionFailed(ExecutionOrder order, String reason) {
        String traceId = getOrCreateTraceId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("executionId", order.getId() != null ? order.getId().toString() : null);
        payload.put("workOrderId", order.getWorkOrderId() != null ? order.getWorkOrderId().toString() : null);
        payload.put("reason", reason != null ? reason : "Execution failed");
        payload.put("timestamp", order.getCompletedAt() != null ? order.getCompletedAt().toString() : null);

        var envelope = EventEnvelope.of("ExecutionFailedEvent", SOURCE_SERVICE, traceId, payload);
        publish(order.getWorkOrderId(), envelope, traceId);
        log.warn("Published ExecutionFailedEvent for OS ID: {} on topic: {}", order.getWorkOrderId(), EXECUTION_EVENTS_TOPIC);
    }

    private void publish(UUID partitionKey, EventEnvelope<Map<String, Object>> envelope, String traceId) {
        String key = partitionKey != null ? partitionKey.toString() : UUID.randomUUID().toString();
        ProducerRecord<String, Object> record = new ProducerRecord<>(EXECUTION_EVENTS_TOPIC, key, envelope);
        KafkaTraceContextUtils.injectTraceContext(record.headers(), traceId, null);
        kafkaTemplate.send(record);
    }

    private String getOrCreateTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
    }
}
