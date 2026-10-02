package br.com.fiap.phase4.exec.domain;

import br.com.fiap.phase4.commons.domain.exception.DomainException;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
import br.com.fiap.phase4.exec.domain.port.out.ExecutionEventPublisherPort;
import br.com.fiap.phase4.exec.domain.port.out.ExecutionOrderRepositoryPort;
import br.com.fiap.phase4.exec.domain.usecase.ExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Execution Domain & Use Case Tests")
class ExecDomainTest {

    private InMemoryExecutionRepository repository;
    private MockExecutionEventPublisher publisher;
    private ExecutionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryExecutionRepository();
        publisher = new MockExecutionEventPublisher();
        service = new ExecutionService(repository, publisher);
    }

    @Test
    @DisplayName("Should successfully enqueue execution order and publish started event")
    void shouldEnqueueExecutionOrder() {
        UUID workOrderId = UUID.randomUUID();

        ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Initial diagnostic");

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getWorkOrderId()).isEqualTo(workOrderId);
        assertThat(order.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
        assertThat(publisher.startedPublished).isTrue();
    }

    @Test
    @DisplayName("Should progress execution order through diagnostic, repair, and completion")
    void shouldProgressExecutionOrder() {
        UUID workOrderId = UUID.randomUUID();
        ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Start");

        service.addChecklistItem(order.getId(), "Check oil filter", true);
        ExecutionOrder diagnosing = service.updateStatus(order.getId(), ExecutionStatus.DIAGNOSIS, "Diagnosing");
        assertThat(diagnosing.getStatus()).isEqualTo(ExecutionStatus.DIAGNOSIS);
        assertThat(diagnosing.getChecklist()).hasSize(1);

        ExecutionOrder completed = service.updateStatus(order.getId(), ExecutionStatus.COMPLETED, "All repairs done");
        assertThat(completed.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(publisher.completedPublished).isTrue();
    }

    @Test
    @DisplayName("Should reject status change after completion")
    void shouldRejectStatusChangeAfterCompletion() {
        UUID workOrderId = UUID.randomUUID();
        ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Start");
        service.updateStatus(order.getId(), ExecutionStatus.COMPLETED, "Done");

        assertThatThrownBy(() -> service.updateStatus(order.getId(), ExecutionStatus.IN_REPAIR, "Reopen"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot change status of a completed execution order");
    }

    static class InMemoryExecutionRepository implements ExecutionOrderRepositoryPort {
        private final List<ExecutionOrder> orders = new ArrayList<>();

        @Override
        public ExecutionOrder save(ExecutionOrder order) {
            orders.removeIf(o -> o.getId().equals(order.getId()));
            orders.add(order);
            return order;
        }

        @Override
        public Optional<ExecutionOrder> findById(UUID id) {
            return orders.stream().filter(o -> o.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<ExecutionOrder> findByWorkOrderId(UUID workOrderId) {
            return orders.stream().filter(o -> o.getWorkOrderId().equals(workOrderId)).findFirst();
        }

        @Override
        public List<ExecutionOrder> findByStatusIn(List<ExecutionStatus> statuses) {
            return orders.stream().filter(o -> statuses.contains(o.getStatus())).toList();
        }
    }

    static class MockExecutionEventPublisher implements ExecutionEventPublisherPort {
        boolean startedPublished = false;
        boolean completedPublished = false;
        boolean failedPublished = false;

        @Override
        public void publishExecutionStarted(ExecutionOrder order) {
            startedPublished = true;
        }

        @Override
        public void publishExecutionCompleted(ExecutionOrder order) {
            completedPublished = true;
        }

        @Override
        public void publishExecutionFailed(ExecutionOrder order, String reason) {
            failedPublished = true;
        }
    }
}
