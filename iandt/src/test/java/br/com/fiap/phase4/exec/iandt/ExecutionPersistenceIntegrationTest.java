package br.com.fiap.phase4.exec.iandt;

import br.com.fiap.phase4.exec.adapter.out.persistence.document.ChecklistItemDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.document.ExecutionOrderMongoDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.document.MaintenanceMaterialDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Execution Order Persistence Integration Tests with MongoDB Testcontainers")
class ExecutionPersistenceIntegrationTest extends ExecIntegrationTest {

    @Test
    @DisplayName("Should persist and retrieve execution order document from real MongoDB container")
    void shouldPersistAndRetrieveExecutionOrder() {
        String id = UUID.randomUUID().toString();
        String workOrderId = UUID.randomUUID().toString();

        ExecutionOrderMongoDocument doc = new ExecutionOrderMongoDocument(
                id,
                workOrderId,
                "TECH-101",
                "QUEUED",
                "Routine checkup",
                List.of(new ChecklistItemDocument("Oil level check", true, Instant.now())),
                List.of(new MaintenanceMaterialDocument("PART-FLT-01", "Oil Filter", 1)),
                Instant.now(),
                Instant.now(),
                null
        );

        ExecutionOrderMongoDocument saved = executionOrderRepository.save(doc);
        assertThat(saved.getId()).isEqualTo(id);

        Optional<ExecutionOrderMongoDocument> found = executionOrderRepository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getWorkOrderId()).isEqualTo(workOrderId);
        assertThat(found.get().getStatus()).isEqualTo("QUEUED");
        assertThat(found.get().getTechnicianId()).isEqualTo("TECH-101");
        assertThat(found.get().getChecklist()).hasSize(1);
        assertThat(found.get().getChecklist().get(0).task()).isEqualTo("Oil level check");
        assertThat(found.get().getMaterials()).hasSize(1);
        assertThat(found.get().getMaterials().get(0).sku()).isEqualTo("PART-FLT-01");
    }

    @Test
    @DisplayName("Should find execution order by workOrderId in MongoDB")
    void shouldFindExecutionOrderByWorkOrderId() {
        String workOrderId = UUID.randomUUID().toString();
        ExecutionOrderMongoDocument doc = new ExecutionOrderMongoDocument(
                UUID.randomUUID().toString(),
                workOrderId,
                "TECH-102",
                "IN_REPAIR",
                "Brake service",
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                null
        );
        executionOrderRepository.save(doc);

        Optional<ExecutionOrderMongoDocument> found = executionOrderRepository.findByWorkOrderId(workOrderId);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("IN_REPAIR");
    }

    @Test
    @DisplayName("Should query execution queue by active statuses in MongoDB")
    void shouldQueryExecutionQueueByActiveStatuses() {
        ExecutionOrderMongoDocument docQueued = new ExecutionOrderMongoDocument(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "TECH-103",
                "QUEUED",
                "Tire rotation",
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                null
        );
        ExecutionOrderMongoDocument docRepair = new ExecutionOrderMongoDocument(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "TECH-103",
                "IN_REPAIR",
                "Engine overhaul",
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                null
        );
        ExecutionOrderMongoDocument docCompleted = new ExecutionOrderMongoDocument(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "TECH-104",
                "COMPLETED",
                "Done",
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        executionOrderRepository.saveAll(List.of(docQueued, docRepair, docCompleted));

        List<ExecutionOrderMongoDocument> activeQueue = executionOrderRepository.findByStatusIn(List.of("QUEUED", "IN_REPAIR"));
        assertThat(activeQueue).hasSize(2);
        assertThat(activeQueue).extracting(ExecutionOrderMongoDocument::getStatus)
                .containsExactlyInAnyOrder("QUEUED", "IN_REPAIR");
    }
}
