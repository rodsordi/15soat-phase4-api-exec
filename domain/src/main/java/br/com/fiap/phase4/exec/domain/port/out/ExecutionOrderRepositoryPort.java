package br.com.fiap.phase4.exec.domain.port.out;

import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExecutionOrderRepositoryPort {
    ExecutionOrder save(ExecutionOrder order);
    Optional<ExecutionOrder> findById(UUID id);
    Optional<ExecutionOrder> findByWorkOrderId(UUID workOrderId);
    List<ExecutionOrder> findByStatusIn(List<ExecutionStatus> statuses);
}
