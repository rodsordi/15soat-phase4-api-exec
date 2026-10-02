package br.com.fiap.phase4.exec.domain.port.in;

import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;

import java.util.List;

public interface ListExecutionQueueUseCase {
    List<ExecutionOrder> getQueue();
}
