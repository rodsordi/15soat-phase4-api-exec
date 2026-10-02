package br.com.fiap.phase4.exec.domain.port.in;

import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;

import java.util.UUID;

public interface UpdateExecutionStatusUseCase {
    ExecutionOrder updateStatus(UUID id, ExecutionStatus status, String notes);
}
