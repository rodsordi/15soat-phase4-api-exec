# Plano de Implementação: Completude do Módulo de Domínio de Execução de Oficina

> **Decisão do Usuário**: Opção 1 - Completude Hexagonal com Use Case de Materiais, Padronização de RFC 7807 e Cobertura 100% TDD
> **Status**: Aprovado para Execução

## Contexto e Objetivo
Alinhar o módulo `domain` do microsserviço `15soat-phase4-api-exec` aos diagramas de sequência do repositório E2E (`materials_and_checklist.md`, `execution_queue_and_repair.md`), ao modelo de dados NoSQL (`MODEL.md`, `README.md`) e aos padrões estabelecidos em `15soat-phase4-api-work-order`:
1. Criar a porta de entrada `AddMaterialUseCase` para registro de peças e insumos consumidos na manutenção física da oficina.
2. Implementar a interface em `ExecutionService`, orquestrando a persistência documental do agregado `ExecutionOrder`.
3. Padronizar `errorCode` em todas as exceções de domínio em `ExecutionOrder` para estrita conformidade com a RFC 7807 Problem Details e ADR-001.
4. Expandir os testes unitários em `ExecDomainTest` cobrindo o ciclo de vida completo de materiais e verificações de arquitetura do ArchUnit.

## Escopo de Alterações

### 1. `domain/src/main/java/br/com/fiap/phase4/exec/domain/port/in/AddMaterialUseCase.java`
Criar a porta de entrada:
```java
package br.com.fiap.phase4.exec.domain.port.in;

import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;

import java.util.UUID;

public interface AddMaterialUseCase {
    ExecutionOrder addMaterial(UUID executionId, String sku, String name, int quantity);
}
```

### 2. `domain/src/main/java/br/com/fiap/phase4/exec/domain/model/ExecutionOrder.java`
Padronizar códigos de erro em `DomainException`:
- `EXECUTION_ALREADY_FINALIZED` para mutações após `COMPLETED` ou `FAILED`.
- `TASK_DESCRIPTION_BLANK` para checklist sem descrição.
- `MATERIAL_INVALID_QUANTITY` para quantidades `<= 0`.

### 3. `domain/src/main/java/br/com/fiap/phase4/exec/domain/usecase/ExecutionService.java`
Implementar `AddMaterialUseCase` e o método `addMaterial`:
```java
@Override
public ExecutionOrder addMaterial(UUID id, String sku, String name, int quantity) {
    ExecutionOrder order = getById(id);
    order.addMaterial(sku, name, quantity);
    return repository.save(order);
}
```

### 4. `domain/src/test/java/br/com/fiap/phase4/exec/domain/ExecDomainTest.java`
Adicionar testes cobrindo:
- Adição com sucesso de materiais consumidos em baia.
- Rejeição de adição de materiais após conclusão da ordem de execução (`EXECUTION_ALREADY_FINALIZED`).
- Validação de quantidade inválida (`quantity <= 0`).
- Validação de regras de arquitetura hexagonal com ArchUnit.
