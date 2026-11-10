package br.com.fiap.phase4.exec.adapter.out.persistence;

import br.com.fiap.phase4.exec.adapter.out.persistence.document.ChecklistItemDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.document.ExecutionOrderMongoDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.document.MaintenanceMaterialDocument;
import br.com.fiap.phase4.exec.adapter.out.persistence.repository.SpringDataExecutionOrderRepository;
import br.com.fiap.phase4.exec.domain.model.ChecklistItem;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
import br.com.fiap.phase4.exec.domain.model.MaintenanceMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionRepositoryAdapterTest {

    @Mock
    private SpringDataExecutionOrderRepository repository;

    @InjectMocks
    private ExecutionRepositoryAdapter adapter;

    private UUID executionId;
    private UUID workOrderId;
    private ExecutionOrder sampleOrder;
    private ExecutionOrderMongoDocument sampleDoc;

    @BeforeEach
    void setUp() {
        executionId = UUID.randomUUID();
        workOrderId = UUID.randomUUID();

        sampleOrder = new ExecutionOrder(
                executionId,
                workOrderId,
                "TECH-101",
                ExecutionStatus.IN_REPAIR,
                "Active repair",
                List.of(ChecklistItem.of("Verify brakes", true)),
                List.of(MaintenanceMaterial.of("PART-01", "Brake Pad", 2)),
                Instant.now(),
                Instant.now(),
                null
        );

        sampleDoc = new ExecutionOrderMongoDocument(
                executionId.toString(),
                workOrderId.toString(),
                "TECH-101",
                ExecutionStatus.IN_REPAIR.name(),
                "Active repair",
                List.of(new ChecklistItemDocument("Verify brakes", true, Instant.now())),
                List.of(new MaintenanceMaterialDocument("PART-01", "Brake Pad", 2)),
                Instant.now(),
                Instant.now(),
                null
        );
    }

    @Test
    @DisplayName("Should save execution order and convert to MongoDB document correctly")
    void shouldSaveExecutionOrder() {
        when(repository.save(any(ExecutionOrderMongoDocument.class))).thenReturn(sampleDoc);

        ExecutionOrder result = adapter.save(sampleOrder);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(executionId);
        assertThat(result.getWorkOrderId()).isEqualTo(workOrderId);
        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.IN_REPAIR);
        assertThat(result.getChecklist()).hasSize(1);
        assertThat(result.getMaterials()).hasSize(1);

        ArgumentCaptor<ExecutionOrderMongoDocument> captor = ArgumentCaptor.forClass(ExecutionOrderMongoDocument.class);
        verify(repository).save(captor.capture());
        ExecutionOrderMongoDocument captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo(executionId.toString());
        assertThat(captured.getWorkOrderId()).isEqualTo(workOrderId.toString());
        assertThat(captured.getStatus()).isEqualTo("IN_REPAIR");
    }

    @Test
    @DisplayName("Should find execution order by ID")
    void shouldFindById() {
        when(repository.findById(executionId.toString())).thenReturn(Optional.of(sampleDoc));

        Optional<ExecutionOrder> result = adapter.findById(executionId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(executionId);
        assertThat(result.get().getWorkOrderId()).isEqualTo(workOrderId);
    }

    @Test
    @DisplayName("Should return empty optional when finding by null ID")
    void shouldReturnEmptyForNullId() {
        Optional<ExecutionOrder> result = adapter.findById(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find execution order by workOrderId")
    void shouldFindByWorkOrderId() {
        when(repository.findByWorkOrderId(workOrderId.toString())).thenReturn(Optional.of(sampleDoc));

        Optional<ExecutionOrder> result = adapter.findByWorkOrderId(workOrderId);

        assertThat(result).isPresent();
        assertThat(result.get().getWorkOrderId()).isEqualTo(workOrderId);
    }

    @Test
    @DisplayName("Should find execution orders by status list")
    void shouldFindByStatusIn() {
        when(repository.findByStatusIn(List.of("QUEUED", "IN_REPAIR"))).thenReturn(List.of(sampleDoc));

        List<ExecutionOrder> result = adapter.findByStatusIn(List.of(ExecutionStatus.QUEUED, ExecutionStatus.IN_REPAIR));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ExecutionStatus.IN_REPAIR);
    }
}
