package br.com.fiap.phase4.exec.domain.port.in;

import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;

import java.util.UUID;

public interface AddChecklistItemUseCase {
    ExecutionOrder addChecklistItem(UUID id, String task, boolean completed);
}
