package br.com.fiap.phase4.exec.adapter.in.web;

import br.com.fiap.phase4.exec.adapter.in.web.api.ExecutionApi;
import br.com.fiap.phase4.exec.adapter.in.web.dto.AddChecklistItemRequest;
import br.com.fiap.phase4.exec.adapter.in.web.dto.AddMaterialRequest;
import br.com.fiap.phase4.exec.adapter.in.web.dto.CreateExecutionRequest;
import br.com.fiap.phase4.exec.adapter.in.web.dto.ExecutionDetailedResponse;
import br.com.fiap.phase4.exec.adapter.in.web.dto.ExecutionSummaryResponse;
import br.com.fiap.phase4.exec.adapter.in.web.dto.PagedExecutionResponse;
import br.com.fiap.phase4.exec.adapter.in.web.dto.UpdateExecutionStatusRequest;
import br.com.fiap.phase4.exec.adapter.in.web.mapper.ExecutionDtoMapper;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
import br.com.fiap.phase4.exec.domain.port.in.AddChecklistItemUseCase;
import br.com.fiap.phase4.exec.domain.port.in.AddMaterialUseCase;
import br.com.fiap.phase4.exec.domain.port.in.GetExecutionOrderUseCase;
import br.com.fiap.phase4.exec.domain.port.in.ListExecutionQueueUseCase;
import br.com.fiap.phase4.exec.domain.port.in.StartExecutionUseCase;
import br.com.fiap.phase4.exec.domain.port.in.UpdateExecutionStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExecutionController implements ExecutionApi {

    private final StartExecutionUseCase startExecutionUseCase;
    private final UpdateExecutionStatusUseCase updateExecutionStatusUseCase;
    private final GetExecutionOrderUseCase getExecutionOrderUseCase;
    private final ListExecutionQueueUseCase listExecutionQueueUseCase;
    private final AddChecklistItemUseCase addChecklistItemUseCase;
    private final AddMaterialUseCase addMaterialUseCase;

    @Override
    public ResponseEntity<ExecutionDetailedResponse> createExecutionOrder(CreateExecutionRequest request) {
        log.info("REST: Enqueuing execution order for workOrder: {}", request.getWorkOrderId());
        ExecutionOrder order = startExecutionUseCase.startExecution(
                request.getWorkOrderId(),
                request.getTechnicianId(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ExecutionDtoMapper.toDetailedResponse(order));
    }

    @Override
    public ResponseEntity<ExecutionDetailedResponse> getExecutionOrderById(UUID id) {
        log.info("REST: Fetching execution order: {}", id);
        ExecutionOrder order = getExecutionOrderUseCase.getById(id);
        return ResponseEntity.ok(ExecutionDtoMapper.toDetailedResponse(order));
    }

    @Override
    public ResponseEntity<ExecutionDetailedResponse> patchExecutionOrder(UUID id, UpdateExecutionStatusRequest request) {
        log.info("REST: Updating execution order: {} to status: {}", id, request.getStatus());
        ExecutionStatus newStatus = ExecutionStatus.valueOf(request.getStatus().name());
        ExecutionOrder updated = updateExecutionStatusUseCase.updateStatus(id, newStatus, request.getNotes());
        return ResponseEntity.ok(ExecutionDtoMapper.toDetailedResponse(updated));
    }

    @Override
    public ResponseEntity<PagedExecutionResponse> getExecutionQueue(OffsetDateTime createdFrom, OffsetDateTime createdTo,
                                                                   Integer page, Integer size) {
        log.info("REST: Listing execution queue");
        List<ExecutionOrder> queue = listExecutionQueueUseCase.getQueue();
        List<ExecutionSummaryResponse> content = queue.stream()
                .map(ExecutionDtoMapper::toSummaryResponse)
                .toList();

        PagedExecutionResponse response = new PagedExecutionResponse()
                .content(content)
                .page(page != null ? page : 0)
                .size(size != null ? size : (content.isEmpty() ? 20 : content.size()))
                .totalElements((long) content.size())
                .totalPages(content.isEmpty() ? 0 : 1);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ExecutionDetailedResponse> addChecklistItem(UUID id, AddChecklistItemRequest request) {
        log.info("REST: Adding checklist item to execution: {}", id);
        boolean completed = Boolean.TRUE.equals(request.getCompleted());
        ExecutionOrder updated = addChecklistItemUseCase.addChecklistItem(id, request.getTask(), completed);
        return ResponseEntity.ok(ExecutionDtoMapper.toDetailedResponse(updated));
    }

    @Override
    public ResponseEntity<ExecutionDetailedResponse> addMaintenanceMaterial(UUID id, AddMaterialRequest request) {
        log.info("REST: Adding maintenance material to execution: {}", id);
        String sku = (request.getSku() != null && !request.getSku().isBlank())
                ? request.getSku()
                : request.getMaterialCode();
        String name = (request.getName() != null && !request.getName().isBlank())
                ? request.getName()
                : request.getDescription();
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;

        ExecutionOrder updated = addMaterialUseCase.addMaterial(id, sku, name, quantity);
        return ResponseEntity.ok(ExecutionDtoMapper.toDetailedResponse(updated));
    }
}
