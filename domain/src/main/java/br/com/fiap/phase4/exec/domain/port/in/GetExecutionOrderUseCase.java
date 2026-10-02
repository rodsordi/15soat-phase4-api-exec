package br.com.fiap.phase4.exec.domain.port.in;

import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;

import java.util.Optional;
import java.util.UUID;

public interface GetExecutionOrderUseCase {
    ExecutionOrder getById(UUID id);
    Optional<ExecutionOrder> getByWorkOrderId(UUID workOrderId);
}
