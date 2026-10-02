package br.com.fiap.phase4.exec.adapter.out.persistence.entity;

import br.com.fiap.phase4.commons.mongo.document.BaseMongoDocument;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "execution_orders")
public class ExecutionOrderMongoDocument extends BaseMongoDocument {

    @Indexed(unique = true)
    private String workOrderId;

    private String technicianId;

    @Indexed
    private String status;

    private String notes;

    private List<ChecklistItemDocument> checklist = new ArrayList<>();

    private Instant completedAt;

    public ExecutionOrderMongoDocument(String id) {
        super(id);
    }
}
