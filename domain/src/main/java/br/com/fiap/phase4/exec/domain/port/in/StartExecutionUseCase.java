package br.com.fiap.phase4.exec.domain.port.in;

import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;

import java.util.UUID;

public interface StartExecutionUseCase {
    ExecutionOrder startExecution(UUID workOrderId, String technicianId, String notes);
}
