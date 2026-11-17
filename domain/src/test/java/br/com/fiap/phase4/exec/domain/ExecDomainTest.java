package br.com.fiap.phase4.exec.domain;

import br.com.fiap.phase4.commons.domain.exception.DomainException;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import br.com.fiap.phase4.exec.domain.model.ChecklistItem;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
import br.com.fiap.phase4.exec.domain.model.MaintenanceMaterial;
import br.com.fiap.phase4.exec.domain.port.out.ExecutionEventPublisherPort;
import br.com.fiap.phase4.exec.domain.port.out.ExecutionOrderRepositoryPort;
import br.com.fiap.phase4.exec.domain.usecase.ExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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

    @Nested
    @DisplayName("Given Execution Lifecycle Progression")
    class GivenExecutionLifecycle {

        @Test
        @DisplayName("Should successfully enqueue execution order and publish started event")
        void shouldEnqueueExecutionOrder() {
            UUID workOrderId = UUID.randomUUID();

            ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Initial diagnostic");

            assertThat(order).isNotNull();
            assertThat(order.getId()).isNotNull();
            assertThat(order.getWorkOrderId()).isEqualTo(workOrderId);
            assertThat(order.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
            assertThat(order.getTechnicianId()).isEqualTo("TECH-01");
            assertThat(order.getNotes()).isEqualTo("Initial diagnostic");
            assertThat(order.getCreatedAt()).isNotNull();
            assertThat(order.getUpdatedAt()).isNotNull();
            assertThat(publisher.startedPublished).isTrue();
        }

        @Test
        @DisplayName("Should return existing execution order if already created for workOrderId (idempotency)")
        void shouldReturnExistingWhenAlreadyCreated() {
            UUID workOrderId = UUID.randomUUID();

            ExecutionOrder first = service.startExecution(workOrderId, "TECH-01", "First");
            ExecutionOrder second = service.startExecution(workOrderId, "TECH-02", "Second");

            assertThat(second.getId()).isEqualTo(first.getId());
        }

        @Test
        @DisplayName("Should progress execution order through diagnostic, repair, and completion")
        void shouldProgressExecutionOrder() {
            UUID workOrderId = UUID.randomUUID();
            ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Start");

            service.addChecklistItem(order.getId(), "Check oil filter", false);
            // Update same item to completed
            service.addChecklistItem(order.getId(), "Check oil filter", true);

            ExecutionOrder diagnosing = service.updateStatus(order.getId(), ExecutionStatus.DIAGNOSIS, "Diagnosing");
            assertThat(diagnosing.getStatus()).isEqualTo(ExecutionStatus.DIAGNOSIS);
            assertThat(diagnosing.getChecklist()).hasSize(1);
            assertThat(diagnosing.getChecklist().get(0).completed()).isTrue();

            ExecutionOrder completed = service.updateStatus(order.getId(), ExecutionStatus.COMPLETED, "All repairs done");
            assertThat(completed.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
            assertThat(completed.getCompletedAt()).isNotNull();
            assertThat(publisher.completedPublished).isTrue();
        }

        @Test
        @DisplayName("Should fail execution order and publish failure event with default reason when notes null")
        void shouldFailExecutionOrderWithDefaultReason() {
            UUID workOrderId = UUID.randomUUID();
            ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Start");

            ExecutionOrder failed = service.updateStatus(order.getId(), ExecutionStatus.FAILED, null);

            assertThat(failed.getStatus()).isEqualTo(ExecutionStatus.FAILED);
            assertThat(publisher.failedPublished).isTrue();
            assertThat(publisher.lastFailureReason).isEqualTo("Execution failed");
        }
    }

    @Nested
    @DisplayName("Given Query Operations")
    class GivenQueryOperations {

        @Test
        @DisplayName("Should retrieve execution order by workOrderId")
        void shouldRetrieveByWorkOrderId() {
            UUID workOrderId = UUID.randomUUID();
            service.startExecution(workOrderId, "TECH-01", "Start");

            Optional<ExecutionOrder> found = service.getByWorkOrderId(workOrderId);

            assertThat(found).isPresent();
            assertThat(found.get().getWorkOrderId()).isEqualTo(workOrderId);
        }

        @Test
        @DisplayName("Should return empty optional when workOrderId not found")
        void shouldReturnEmptyWhenWorkOrderIdNotFound() {
            Optional<ExecutionOrder> found = service.getByWorkOrderId(UUID.randomUUID());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should retrieve execution queue with queued items")
        void shouldRetrieveExecutionQueue() {
            UUID wo1 = UUID.randomUUID();
            UUID wo2 = UUID.randomUUID();
            service.startExecution(wo1, "TECH-01", "Order 1");
            service.startExecution(wo2, "TECH-02", "Order 2");

            List<ExecutionOrder> queue = service.getQueue();

            assertThat(queue).hasSize(2);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when getById does not find order")
        void shouldThrowResourceNotFoundExceptionOnGetById() {
            UUID id = UUID.randomUUID();

            assertThatThrownBy(() -> service.getById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ExecutionOrder")
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    @DisplayName("Given Materials and Checklists")
    class GivenMaterialsAndChecklists {

        @Test
        @DisplayName("Should successfully add maintenance materials to execution order")
        void shouldAddMaintenanceMaterial() {
            UUID workOrderId = UUID.randomUUID();
            ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Start");

            ExecutionOrder updated = service.addMaterial(order.getId(), "PART-BRK-01", "Pastilha de Freio", 2);

            assertThat(updated.getMaterials()).hasSize(1);
            MaintenanceMaterial material = updated.getMaterials().get(0);
            assertThat(material.sku()).isEqualTo("PART-BRK-01");
            assertThat(material.name()).isEqualTo("Pastilha de Freio");
            assertThat(material.quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should reject adding material with quantity less than or equal to zero")
        void shouldRejectMaterialWithInvalidQuantity() {
            assertThatThrownBy(() -> MaintenanceMaterial.of("PART-01", "Filtro", 0))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Quantity must be greater than zero")
                    .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo("MATERIAL_INVALID_QUANTITY"));

            assertThatThrownBy(() -> MaintenanceMaterial.of("PART-01", "Filtro", -1))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Quantity must be greater than zero");
        }

        @Test
        @DisplayName("Should reject blank checklist task description")
        void shouldRejectBlankChecklistTask() {
            UUID workOrderId = UUID.randomUUID();
            ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Start");

            assertThatThrownBy(() -> service.addChecklistItem(order.getId(), "   ", true))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Task description cannot be blank")
                    .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo("TASK_DESCRIPTION_BLANK"));
        }

        @Test
        @DisplayName("Should reject status change after completion")
        void shouldRejectStatusChangeAfterCompletion() {
            UUID workOrderId = UUID.randomUUID();
            ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Start");
            service.updateStatus(order.getId(), ExecutionStatus.COMPLETED, "Done");

            assertThatThrownBy(() -> service.updateStatus(order.getId(), ExecutionStatus.IN_REPAIR, "Reopen"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot change status of a finalized execution order")
                    .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo("EXECUTION_ALREADY_FINALIZED"));
        }

        @Test
        @DisplayName("Should reject adding material after completion")
        void shouldRejectAddingMaterialAfterCompletion() {
            UUID workOrderId = UUID.randomUUID();
            ExecutionOrder order = service.startExecution(workOrderId, "TECH-01", "Start");
            service.updateStatus(order.getId(), ExecutionStatus.COMPLETED, "Done");

            assertThatThrownBy(() -> service.addMaterial(order.getId(), "PART-01", "Filtro", 1))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Cannot add materials to a finalized execution order")
                    .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo("EXECUTION_ALREADY_FINALIZED"));
        }

        @Test
        @DisplayName("Should reconstruct ChecklistItem and ExecutionOrder with all fields")
        void shouldReconstructDomainEntities() {
            ChecklistItem item = ChecklistItem.of("Check brakes", true);
            assertThat(item.task()).isEqualTo("Check brakes");
            assertThat(item.completed()).isTrue();
            assertThat(item.completedAt()).isNotNull();

            UUID id = UUID.randomUUID();
            UUID workOrderId = UUID.randomUUID();
            Instant now = Instant.now();

            ExecutionOrder reconstructed = new ExecutionOrder(
                    id,
                    workOrderId,
                    "TECH-99",
                    ExecutionStatus.WAITING_PARTS,
                    "Waiting for brake discs",
                    List.of(item),
                    List.of(),
                    now,
                    now,
                    null
            );

            assertThat(reconstructed.getId()).isEqualTo(id);
            assertThat(reconstructed.getWorkOrderId()).isEqualTo(workOrderId);
            assertThat(reconstructed.getTechnicianId()).isEqualTo("TECH-99");
            assertThat(reconstructed.getStatus()).isEqualTo(ExecutionStatus.WAITING_PARTS);
            assertThat(reconstructed.getNotes()).isEqualTo("Waiting for brake discs");
        }
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
        String lastFailureReason = null;

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
            lastFailureReason = reason;
        }
    }
}
