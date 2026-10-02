package br.com.fiap.phase4.exec.domain.model;

import br.com.fiap.phase4.commons.domain.exception.DomainException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExecutionOrder {

    private UUID id;
    private UUID workOrderId;
    private String technicianId;
    private ExecutionStatus status;
    private String notes;

    @Getter(AccessLevel.NONE)
    private List<ChecklistItem> checklist = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public ExecutionOrder(UUID id, UUID workOrderId, String technicianId, ExecutionStatus status,
                          String notes, List<ChecklistItem> checklist,
                          Instant createdAt, Instant updatedAt, Instant completedAt) {
        this.id = Objects.requireNonNull(id, "Execution ID cannot be null");
        this.workOrderId = Objects.requireNonNull(workOrderId, "WorkOrder ID cannot be null");
        this.technicianId = technicianId;
        this.status = status != null ? status : ExecutionStatus.QUEUED;
        this.notes = notes;
        this.checklist = checklist != null ? new ArrayList<>(checklist) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.completedAt = completedAt;
    }

    public static ExecutionOrder enqueue(UUID workOrderId, String technicianId, String notes) {
        Instant now = Instant.now();
        return new ExecutionOrder(
                UUID.randomUUID(),
                workOrderId,
                technicianId,
                ExecutionStatus.QUEUED,
                notes,
                new ArrayList<>(),
                now,
                now,
                null
        );
    }

    public void updateStatus(ExecutionStatus newStatus, String updatedNotes) {
        Objects.requireNonNull(newStatus, "New status cannot be null");
        if (this.status == ExecutionStatus.COMPLETED) {
            throw new DomainException("Cannot change status of a completed execution order");
        }
        if (this.status == ExecutionStatus.FAILED) {
            throw new DomainException("Cannot change status of a failed execution order");
        }

        this.status = newStatus;
        if (updatedNotes != null && !updatedNotes.isBlank()) {
            this.notes = updatedNotes;
        }
        this.updatedAt = Instant.now();

        if (newStatus == ExecutionStatus.COMPLETED || newStatus == ExecutionStatus.FAILED) {
            this.completedAt = Instant.now();
        }
    }

    public void addOrUpdateChecklistItem(String task, boolean completed) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (task.isBlank()) {
            throw new DomainException("Task description cannot be blank");
        }
        if (this.status == ExecutionStatus.COMPLETED || this.status == ExecutionStatus.FAILED) {
            throw new DomainException("Cannot alter checklist of a finalized execution order");
        }

        checklist.removeIf(item -> item.task().equalsIgnoreCase(task.trim()));
        checklist.add(ChecklistItem.of(task.trim(), completed));
        this.updatedAt = Instant.now();
    }

    public void assignTechnician(String technicianId) {
        this.technicianId = Objects.requireNonNull(technicianId, "Technician ID cannot be null");
        this.updatedAt = Instant.now();
    }

    public List<ChecklistItem> getChecklist() {
        return Collections.unmodifiableList(checklist);
    }
}
