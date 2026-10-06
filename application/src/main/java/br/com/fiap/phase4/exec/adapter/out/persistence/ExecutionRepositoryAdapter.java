package br.com.fiap.phase4.exec.adapter.out.persistence;

import br.com.fiap.phase4.exec.adapter.out.persistence.entity.ChecklistItemDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.entity.ExecutionOrderMongoDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.repository.SpringDataExecutionOrderRepository;
import br.com.fiap.phase4.exec.domain.model.ChecklistItem;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
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
        ExecutionOrderMongoDocument doc = toDocument(order);
        ExecutionOrderMongoDocument saved = repository.save(doc);
        return toDomain(saved);
    }

    @Override
    public Optional<ExecutionOrder> findById(UUID id) {
        return repository.findById(id.toString()).map(this::toDomain);
    }

    @Override
    public Optional<ExecutionOrder> findByWorkOrderId(UUID workOrderId) {
        return repository.findByWorkOrderId(workOrderId.toString()).map(this::toDomain);
    }

    @Override
    public List<ExecutionOrder> findByStatusIn(List<ExecutionStatus> statuses) {
        List<String> statusStrings = statuses.stream().map(Enum::name).toList();
        return repository.findByStatusIn(statusStrings).stream()
                .map(this::toDomain)
                .toList();
    }

    private ExecutionOrderMongoDocument toDocument(ExecutionOrder domain) {
        ExecutionOrderMongoDocument doc = new ExecutionOrderMongoDocument(domain.getId().toString());
        doc.setWorkOrderId(domain.getWorkOrderId().toString());
        doc.setTechnicianId(domain.getTechnicianId());
        doc.setStatus(domain.getStatus().name());
        doc.setNotes(domain.getNotes());
        doc.setCreatedAt(domain.getCreatedAt());
        doc.setUpdatedAt(domain.getUpdatedAt());
        doc.setCompletedAt(domain.getCompletedAt());

        if (domain.getChecklist() != null) {
            doc.setChecklist(domain.getChecklist().stream()
                    .map(item -> new ChecklistItemDocument(item.task(), item.completed(), item.completedAt()))
                    .toList());
        }

        if (domain.getMaterials() != null) {
            doc.setMaterials(domain.getMaterials().stream()
                    .map(mat -> new br.com.fiap.phase4.exec.adapter.out.persistence.entity.MaintenanceMaterialDocument(mat.sku(), mat.name(), mat.quantity()))
                    .toList());
        }
        return doc;
    }

    private ExecutionOrder toDomain(ExecutionOrderMongoDocument doc) {
        List<ChecklistItem> checklist = doc.getChecklist() == null ? List.of() :
                doc.getChecklist().stream()
                        .map(item -> new ChecklistItem(item.getTask(), item.isCompleted(), item.getCompletedAt()))
                        .toList();

        List<br.com.fiap.phase4.exec.domain.model.MaintenanceMaterial> materials = doc.getMaterials() == null ? List.of() :
                doc.getMaterials().stream()
                        .map(m -> new br.com.fiap.phase4.exec.domain.model.MaintenanceMaterial(m.getSku(), m.getName(), m.getQuantity()))
                        .toList();

        return new ExecutionOrder(
                UUID.fromString(doc.getId()),
                UUID.fromString(doc.getWorkOrderId()),
                doc.getTechnicianId(),
                ExecutionStatus.valueOf(doc.getStatus()),
                doc.getNotes(),
                checklist,
                materials,
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                doc.getCompletedAt()
        );
    }
}
