package br.com.fiap.phase4.exec.adapter.in.web.mapper;

import br.com.fiap.phase4.exec.adapter.in.web.dto.ChecklistItemDto;
import br.com.fiap.phase4.exec.adapter.in.web.dto.ExecutionDetailedResponse;
import br.com.fiap.phase4.exec.adapter.in.web.dto.ExecutionDetailedResponseLinks;
import br.com.fiap.phase4.exec.adapter.in.web.dto.ExecutionSummaryResponse;
import br.com.fiap.phase4.exec.adapter.in.web.dto.Link;
import br.com.fiap.phase4.exec.adapter.in.web.dto.MaintenanceMaterialDto;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;

import java.time.ZoneOffset;
import java.util.List;

public final class ExecutionDtoMapper {

    private ExecutionDtoMapper() {}

    public static ExecutionDetailedResponse toDetailedResponse(ExecutionOrder order) {
        if (order == null) {
            return null;
        }

        List<ChecklistItemDto> checklistDtos = order.getChecklist().stream()
                .map(item -> new ChecklistItemDto()
                        .task(item.task())
                        .completed(item.completed())
                        .completedAt(item.completedAt() != null ? item.completedAt().atOffset(ZoneOffset.UTC) : null))
                .toList();

        List<MaintenanceMaterialDto> materialDtos = order.getMaterials().stream()
                .map(mat -> new MaintenanceMaterialDto()
                        .sku(mat.sku())
                        .name(mat.name())
                        .quantity(mat.quantity()))
                .toList();

        ExecutionDetailedResponse response = new ExecutionDetailedResponse()
                .id(order.getId())
                .workOrderId(order.getWorkOrderId())
                .technicianId(order.getTechnicianId())
                .notes(order.getNotes())
                .checklist(checklistDtos)
                .materials(materialDtos)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
                .updatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().atOffset(ZoneOffset.UTC) : null)
                .completedAt(order.getCompletedAt() != null ? order.getCompletedAt().atOffset(ZoneOffset.UTC) : null);

        if (order.getStatus() != null) {
            try {
                response.setStatus(ExecutionDetailedResponse.StatusEnum.fromValue(order.getStatus().name()));
            } catch (Exception ignored) {}
        }

        if (order.getId() != null) {
            ExecutionDetailedResponseLinks links = new ExecutionDetailedResponseLinks()
                    .self(new Link().href("/api/v1/executions/" + order.getId()));
            if (order.getWorkOrderId() != null) {
                links.setWorkOrder(new Link().href("/api/v1/work-orders/" + order.getWorkOrderId()));
            }
            response.setLinks(links);
        }

        return response;
    }

    public static ExecutionSummaryResponse toSummaryResponse(ExecutionOrder order) {
        if (order == null) {
            return null;
        }

        ExecutionSummaryResponse response = new ExecutionSummaryResponse()
                .id(order.getId())
                .workOrderId(order.getWorkOrderId())
                .technicianId(order.getTechnicianId())
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().atOffset(ZoneOffset.UTC) : null);

        if (order.getStatus() != null) {
            try {
                response.setStatus(ExecutionSummaryResponse.StatusEnum.fromValue(order.getStatus().name()));
            } catch (Exception ignored) {}
        }

        if (order.getId() != null) {
            ExecutionDetailedResponseLinks links = new ExecutionDetailedResponseLinks()
                    .self(new Link().href("/api/v1/executions/" + order.getId()));
            if (order.getWorkOrderId() != null) {
                links.setWorkOrder(new Link().href("/api/v1/work-orders/" + order.getWorkOrderId()));
            }
            response.setLinks(links);
        }

        return response;
    }
}
