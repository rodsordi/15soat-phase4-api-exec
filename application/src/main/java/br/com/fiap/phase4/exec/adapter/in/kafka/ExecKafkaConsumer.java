package br.com.fiap.phase4.exec.adapter.in.kafka;

import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.commons.kafka.tracing.KafkaTraceContextUtils;
import br.com.fiap.phase4.exec.domain.port.in.StartExecutionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecKafkaConsumer {

    private final StartExecutionUseCase startExecutionUseCase;

    @KafkaListener(topics = "payment-events", groupId = "exec-payment-group")
    public void consumePaymentEvents(ConsumerRecord<String, Object> record) {
        String traceId = KafkaTraceContextUtils.extractTraceId(record.headers());
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }

        try {
            Object rawValue = record.value();
            if (rawValue == null) {
                return;
            }

            Map<String, Object> payload = null;
            String eventType = null;

            if (rawValue instanceof EventEnvelope<?> envelope) {
                if (envelope.metadata() != null) {
                    eventType = envelope.metadata().eventType();
                }
                if (envelope.payload() instanceof Map<?, ?> map) {
                    payload = (Map<String, Object>) map;
                }
            } else if (rawValue instanceof Map<?, ?> map) {
                Map<String, Object> mapValue = (Map<String, Object>) map;
                if (mapValue.get("metadata") instanceof Map<?, ?> meta) {
                    eventType = (String) meta.get("eventType");
                }
                if (mapValue.get("payload") instanceof Map<?, ?> inner) {
                    payload = (Map<String, Object>) inner;
                } else {
                    payload = mapValue;
                }
            }

            log.info("Received event on payment-events: {} [traceId: {}]", eventType, traceId);

            if (eventType == null || "PaymentConfirmedEvent".equalsIgnoreCase(eventType) || "PaymentConfirmed".equalsIgnoreCase(eventType)) {
                if (payload != null && payload.get("workOrderId") != null) {
                    UUID workOrderId = UUID.fromString(payload.get("workOrderId").toString());
                    log.info("Saga Progression: Enqueuing execution order for workOrderId: {}", workOrderId);
                    startExecutionUseCase.startExecution(workOrderId, null, "Auto-enqueued on payment confirmation");
                }
            }
        } finally {
            MDC.remove("traceId");
        }
    }
}
