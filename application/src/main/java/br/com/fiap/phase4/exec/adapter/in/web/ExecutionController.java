package br.com.fiap.phase4.exec.adapter.in.web;

import br.com.fiap.phase4.exec.adapter.in.web.api.ExecutionApi;
import br.com.fiap.phase4.exec.adapter.in.web.dto.AddChecklistItemRequest;
import br.com.fiap.phase4.exec.adapter.in.web.dto.ChecklistItemDto;
import br.com.fiap.phase4.exec.adapter.in.web.dto.CreateExecutionRequest;
import br.com.fiap.phase4.exec.adapter.in.web.dto.ExecutionResponse;
import br.com.fiap.phase4.exec.adapter.in.web.dto.UpdateExecutionStatusRequest;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
import br.com.fiap.phase4.exec.domain.port.in.AddChecklistItemUseCase;
import br.com.fiap.phase4.exec.domain.port.in.GetExecutionOrderUseCase;
import br.com.fiap.phase4.exec.domain.port.in.ListExecutionQueueUseCase;
import br.com.fiap.phase4.exec.domain.port.in.StartExecutionUseCase;
import br.com.fiap.phase4.exec.domain.port.in.UpdateExecutionStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
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

    @Override
    public ResponseEntity<ExecutionResponse> createExecutionOrder(CreateExecutionRequest request) {
        ExecutionOrder order = startExecutionUseCase.startExecution(
                request.getWorkOrderId(),
                request.getTechnicianId(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    @Override
    public ResponseEntity<ExecutionResponse> getExecutionOrderById(UUID id) {
        ExecutionOrder order = getExecutionOrderUseCase.getById(id);
        return ResponseEntity.ok(toResponse(order));
    }

    @Override
    public ResponseEntity<List<ExecutionResponse>> getExecutionQueue() {
        List<ExecutionOrder> queue = listExecutionQueueUseCase.getQueue();
        List<ExecutionResponse> responses = queue.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<ExecutionResponse> updateExecutionStatus(UUID id, UpdateExecutionStatusRequest request) {
        ExecutionStatus newStatus = ExecutionStatus.valueOf(request.getStatus().getValue());
        ExecutionOrder updated = updateExecutionStatusUseCase.updateStatus(id, newStatus, request.getNotes());
        return ResponseEntity.ok(toResponse(updated));
    }

    @Override
    public ResponseEntity<ExecutionResponse> addChecklistItem(UUID id, AddChecklistItemRequest request) {
        ExecutionOrder updated = addChecklistItemUseCase.addChecklistItem(id, request.getTask(), Boolean.TRUE.equals(request.getCompleted()));
        return ResponseEntity.ok(toResponse(updated));
    }

    private ExecutionResponse toResponse(ExecutionOrder domain) {
        ExecutionResponse response = new ExecutionResponse();
        response.setId(domain.getId());
        response.setWorkOrderId(domain.getWorkOrderId());
        response.setTechnicianId(domain.getTechnicianId());
        response.setStatus(ExecutionResponse.StatusEnum.fromValue(domain.getStatus().name()));
        response.setNotes(domain.getNotes());
        response.setCreatedAt(domain.getCreatedAt().atOffset(ZoneOffset.UTC));
        response.setUpdatedAt(domain.getUpdatedAt().atOffset(ZoneOffset.UTC));
        if (domain.getCompletedAt() != null) {
            response.setCompletedAt(domain.getCompletedAt().atOffset(ZoneOffset.UTC));
        }

        if (domain.getChecklist() != null) {
            response.setChecklist(domain.getChecklist().stream().map(item -> {
                ChecklistItemDto dto = new ChecklistItemDto();
                dto.setTask(item.task());
                dto.setCompleted(item.completed());
                if (item.completedAt() != null) {
                    dto.setCompletedAt(item.completedAt().atOffset(ZoneOffset.UTC));
                }
                return dto;
            }).toList());
        }

        return response;
    }
}
