package br.com.fiap.phase4.exec.adapter.in.kafka;

import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.commons.kafka.tracing.KafkaTraceContextUtils;
import br.com.fiap.phase4.exec.domain.port.in.StartExecutionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    public void consumePaymentEvents(ConsumerRecord<String, EventEnvelope<Map<String, Object>>> record) {
        String traceId = KafkaTraceContextUtils.extractTraceId(record.headers());
        EventEnvelope<Map<String, Object>> envelope = record.value();

        if (envelope == null || envelope.metadata() == null) {
            return;
        }

        String eventType = envelope.metadata().eventType();
        log.info("Received event on payment-events: {} [traceId: {}]", eventType, traceId);

        if ("PaymentConfirmedEvent".equalsIgnoreCase(eventType)) {
            UUID workOrderId = UUID.fromString(envelope.payload().get("workOrderId").toString());
            log.info("Saga Step: Payment confirmed. Enqueueing Work Order {} into workshop execution queue", workOrderId);
            startExecutionUseCase.startExecution(workOrderId, null, "Auto-enqueued after payment confirmation");
        }
    }
}
