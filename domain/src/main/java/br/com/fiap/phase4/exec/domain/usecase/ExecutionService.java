package br.com.fiap.phase4.exec.domain.usecase;

import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
import br.com.fiap.phase4.exec.domain.port.in.AddChecklistItemUseCase;
import br.com.fiap.phase4.exec.domain.port.in.GetExecutionOrderUseCase;
import br.com.fiap.phase4.exec.domain.port.in.ListExecutionQueueUseCase;
import br.com.fiap.phase4.exec.domain.port.in.StartExecutionUseCase;
import br.com.fiap.phase4.exec.domain.port.in.UpdateExecutionStatusUseCase;
import br.com.fiap.phase4.exec.domain.port.out.ExecutionEventPublisherPort;
import br.com.fiap.phase4.exec.domain.port.out.ExecutionOrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExecutionService implements
        StartExecutionUseCase,
        UpdateExecutionStatusUseCase,
        GetExecutionOrderUseCase,
        ListExecutionQueueUseCase,
        AddChecklistItemUseCase {

    private final ExecutionOrderRepositoryPort repository;
    private final ExecutionEventPublisherPort eventPublisher;

    @Override
    public ExecutionOrder startExecution(UUID workOrderId, String technicianId, String notes) {
        // Idempotency: If execution order already exists for this workOrderId, return existing
        Optional<ExecutionOrder> existing = repository.findByWorkOrderId(workOrderId);
        if (existing.isPresent()) {
            return existing.get();
        }

        ExecutionOrder order = ExecutionOrder.enqueue(workOrderId, technicianId, notes);
        ExecutionOrder saved = repository.save(order);
        eventPublisher.publishExecutionStarted(saved);
        return saved;
    }

    @Override
    public ExecutionOrder updateStatus(UUID id, ExecutionStatus newStatus, String notes) {
        ExecutionOrder order = getById(id);
        order.updateStatus(newStatus, notes);
        ExecutionOrder saved = repository.save(order);

        if (newStatus == ExecutionStatus.COMPLETED) {
            eventPublisher.publishExecutionCompleted(saved);
        } else if (newStatus == ExecutionStatus.FAILED) {
            eventPublisher.publishExecutionFailed(saved, notes != null ? notes : "Execution failed");
        }

        return saved;
    }

    @Override
    public ExecutionOrder getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExecutionOrder", id));
    }

    @Override
    public Optional<ExecutionOrder> getByWorkOrderId(UUID workOrderId) {
        return repository.findByWorkOrderId(workOrderId);
    }

    @Override
    public List<ExecutionOrder> getQueue() {
        return repository.findByStatusIn(List.of(
            ExecutionStatus.QUEUED,
            ExecutionStatus.DIAGNOSIS,
            ExecutionStatus.IN_REPAIR,
            ExecutionStatus.WAITING_PARTS,
            ExecutionStatus.QUALITY_CHECK
        ));
    }

    @Override
    public ExecutionOrder addChecklistItem(UUID id, String task, boolean completed) {
        ExecutionOrder order = getById(id);
        order.addOrUpdateChecklistItem(task, completed);
        return repository.save(order);
    }
}
