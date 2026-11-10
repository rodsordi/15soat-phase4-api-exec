package br.com.fiap.phase4.exec.domain.port.in;

import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;

import java.util.UUID;

public interface AddMaterialUseCase {
    ExecutionOrder addMaterial(UUID executionId, String sku, String name, int quantity);
}
