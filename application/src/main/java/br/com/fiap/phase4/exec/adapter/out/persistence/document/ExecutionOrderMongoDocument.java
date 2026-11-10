package br.com.fiap.phase4.exec.adapter.out.persistence.document;

import br.com.fiap.phase4.commons.mongo.document.BaseMongoDocument;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "execution_orders")
@Getter
@Setter
@NoArgsConstructor
public class ExecutionOrderMongoDocument extends BaseMongoDocument {

    @Indexed(unique = true)
    private String workOrderId;

    private String technicianId;

    @Indexed
    private String status;

    private String notes;

    private List<ChecklistItemDocument> checklist = new ArrayList<>();

    private List<MaintenanceMaterialDocument> materials = new ArrayList<>();

    private Instant completedAt;

    public ExecutionOrderMongoDocument(String id, String workOrderId, String technicianId,
                                       String status, String notes,
                                       List<ChecklistItemDocument> checklist,
                                       List<MaintenanceMaterialDocument> materials,
                                       Instant createdAt, Instant updatedAt, Instant completedAt) {
        super(id);
        this.workOrderId = workOrderId;
        this.technicianId = technicianId;
        this.status = status;
        this.notes = notes;
        this.checklist = checklist != null ? new ArrayList<>(checklist) : new ArrayList<>();
        this.materials = materials != null ? new ArrayList<>(materials) : new ArrayList<>();
        setCreatedAt(createdAt != null ? createdAt : Instant.now());
        setUpdatedAt(updatedAt != null ? updatedAt : Instant.now());
        this.completedAt = completedAt;
    }
}
