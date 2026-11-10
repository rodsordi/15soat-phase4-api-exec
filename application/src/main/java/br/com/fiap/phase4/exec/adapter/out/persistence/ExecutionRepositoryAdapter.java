package br.com.fiap.phase4.exec.adapter.out.persistence;

import br.com.fiap.phase4.exec.adapter.out.persistence.document.ChecklistItemDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.document.ExecutionOrderMongoDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.document.MaintenanceMaterialDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.repository.SpringDataExecutionOrderRepository;
import br.com.fiap.phase4.exec.domain.model.ChecklistItem;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
import br.com.fiap.phase4.exec.domain.model.MaintenanceMaterial;
import br.com.fiap.phase4.exec.domain.port.out.ExecutionOrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExecutionRepositoryAdapter implements ExecutionOrderRepositoryPort {

    private final SpringDataExecutionOrderRepository repository;

    @Override
    public ExecutionOrder save(ExecutionOrder order) {
        ExecutionOrderMongoDocument document = toDocument(order);
        ExecutionOrderMongoDocument saved = repository.save(document);
        return toDomain(saved);
    }

    @Override
    public Optional<ExecutionOrder> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id.toString()).map(this::toDomain);
    }

    @Override
    public Optional<ExecutionOrder> findByWorkOrderId(UUID workOrderId) {
        if (workOrderId == null) {
            return Optional.empty();
        }
        return repository.findByWorkOrderId(workOrderId.toString()).map(this::toDomain);
    }

    @Override
    public List<ExecutionOrder> findByStatusIn(List<ExecutionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        List<String> statusNames = statuses.stream().map(Enum::name).toList();
        return repository.findByStatusIn(statusNames).stream()
                .map(this::toDomain)
                .toList();
    }

    private ExecutionOrderMongoDocument toDocument(ExecutionOrder order) {
        List<ChecklistItemDocument> checklistDocs = order.getChecklist().stream()
                .map(item -> new ChecklistItemDocument(item.task(), item.completed(), item.completedAt()))
                .toList();

        List<MaintenanceMaterialDocument> materialDocs = order.getMaterials().stream()
                .map(mat -> new MaintenanceMaterialDocument(mat.sku(), mat.name(), mat.quantity()))
                .toList();

        return new ExecutionOrderMongoDocument(
                order.getId() != null ? order.getId().toString() : UUID.randomUUID().toString(),
                order.getWorkOrderId() != null ? order.getWorkOrderId().toString() : null,
                order.getTechnicianId(),
                order.getStatus() != null ? order.getStatus().name() : ExecutionStatus.QUEUED.name(),
                order.getNotes(),
                checklistDocs,
                materialDocs,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCompletedAt()
        );
    }

    private ExecutionOrder toDomain(ExecutionOrderMongoDocument doc) {
        List<ChecklistItem> checklist = doc.getChecklist() != null
                ? doc.getChecklist().stream()
                    .map(item -> new ChecklistItem(item.task(), item.completed(), item.completedAt()))
                    .toList()
                : List.of();

        List<MaintenanceMaterial> materials = doc.getMaterials() != null
                ? doc.getMaterials().stream()
                    .map(mat -> new MaintenanceMaterial(mat.sku(), mat.name(), mat.quantity()))
                    .toList()
                : List.of();

        ExecutionStatus status;
        try {
            status = doc.getStatus() != null ? ExecutionStatus.valueOf(doc.getStatus()) : ExecutionStatus.QUEUED;
        } catch (IllegalArgumentException e) {
            status = ExecutionStatus.QUEUED;
        }

        UUID id = doc.getId() != null ? UUID.fromString(doc.getId()) : UUID.randomUUID();
        UUID workOrderId = doc.getWorkOrderId() != null ? UUID.fromString(doc.getWorkOrderId()) : UUID.randomUUID();

        return new ExecutionOrder(
                id,
                workOrderId,
                doc.getTechnicianId(),
                status,
                doc.getNotes(),
                checklist,
                materials,
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                doc.getCompletedAt()
        );
    }
}
