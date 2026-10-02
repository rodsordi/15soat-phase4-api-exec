package br.com.fiap.phase4.exec.domain.port.out;

import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;

public interface ExecutionEventPublisherPort {
    void publishExecutionStarted(ExecutionOrder order);
    void publishExecutionCompleted(ExecutionOrder order);
    void publishExecutionFailed(ExecutionOrder order, String reason);
}
