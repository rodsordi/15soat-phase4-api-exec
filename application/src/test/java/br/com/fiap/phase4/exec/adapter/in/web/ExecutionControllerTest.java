package br.com.fiap.phase4.exec.adapter.in.web;

import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import br.com.fiap.phase4.commons.web.advice.CommonsExceptionAdvice;
import br.com.fiap.phase4.exec.adapter.in.web.advice.ExecutionExceptionHandler;
import br.com.fiap.phase4.exec.adapter.in.web.dto.AddChecklistItemRequest;
import br.com.fiap.phase4.exec.adapter.in.web.dto.AddMaterialRequest;
import br.com.fiap.phase4.exec.adapter.in.web.dto.CreateExecutionRequest;
import br.com.fiap.phase4.exec.adapter.in.web.dto.UpdateExecutionStatusRequest;
import br.com.fiap.phase4.exec.domain.model.ChecklistItem;
import br.com.fiap.phase4.exec.domain.model.ExecutionOrder;
import br.com.fiap.phase4.exec.domain.model.ExecutionStatus;
import br.com.fiap.phase4.exec.domain.model.MaintenanceMaterial;
import br.com.fiap.phase4.exec.domain.port.in.AddChecklistItemUseCase;
import br.com.fiap.phase4.exec.domain.port.in.AddMaterialUseCase;
import br.com.fiap.phase4.exec.domain.port.in.GetExecutionOrderUseCase;
import br.com.fiap.phase4.exec.domain.port.in.ListExecutionQueueUseCase;
import br.com.fiap.phase4.exec.domain.port.in.StartExecutionUseCase;
import br.com.fiap.phase4.exec.domain.port.in.UpdateExecutionStatusUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Execution Controller Web Layer Unit Tests")
class ExecutionControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private StartExecutionUseCase startExecutionUseCase;

    @Mock
    private UpdateExecutionStatusUseCase updateExecutionStatusUseCase;

    @Mock
    private GetExecutionOrderUseCase getExecutionOrderUseCase;

    @Mock
    private ListExecutionQueueUseCase listExecutionQueueUseCase;

    @Mock
    private AddChecklistItemUseCase addChecklistItemUseCase;

    @Mock
    private AddMaterialUseCase addMaterialUseCase;

    @InjectMocks
    private ExecutionController executionController;

    private UUID executionId;
    private UUID workOrderId;
    private ExecutionOrder sampleOrder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(executionController)
                .setControllerAdvice(new CommonsExceptionAdvice(), new ExecutionExceptionHandler())
                .build();

        executionId = UUID.randomUUID();
        workOrderId = UUID.randomUUID();
        sampleOrder = new ExecutionOrder(
                executionId,
                workOrderId,
                "TECH-101",
                ExecutionStatus.QUEUED,
                "Vehicle intake",
                List.of(ChecklistItem.of("Verify oil level", true)),
                List.of(MaintenanceMaterial.of("PART-01", "Brake Pad", 2)),
                Instant.now(),
                Instant.now(),
                null
        );
    }

    @Nested
    @DisplayName("POST /api/v1/executions - Create execution order")
    class CreateExecutionOrderTests {

        @Test
        @DisplayName("Should create execution order successfully and return 201 Created")
        void shouldCreateExecutionOrder() throws Exception {
            when(startExecutionUseCase.startExecution(eq(workOrderId), eq("TECH-101"), anyString()))
                    .thenReturn(sampleOrder);

            CreateExecutionRequest request = new CreateExecutionRequest(workOrderId)
                    .technicianId("TECH-101")
                    .notes("Intake notes");

            mockMvc.perform(post("/api/v1/executions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(executionId.toString()))
                    .andExpect(jsonPath("$.workOrderId").value(workOrderId.toString()))
                    .andExpect(jsonPath("$.status").value("QUEUED"))
                    .andExpect(jsonPath("$.technicianId").value("TECH-101"));

            verify(startExecutionUseCase).startExecution(eq(workOrderId), eq("TECH-101"), anyString());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/executions/{id} - Query execution by ID")
    class GetExecutionOrderByIdTests {

        @Test
        @DisplayName("Should return 200 OK when execution order is found")
        void shouldReturnOrderWhenFound() throws Exception {
            when(getExecutionOrderUseCase.getById(executionId)).thenReturn(sampleOrder);

            mockMvc.perform(get("/api/v1/executions/{id}", executionId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(executionId.toString()))
                    .andExpect(jsonPath("$.status").value("QUEUED"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when execution order does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            when(getExecutionOrderUseCase.getById(executionId))
                    .thenThrow(new ResourceNotFoundException("ExecutionOrder", executionId));

            mockMvc.perform(get("/api/v1/executions/{id}", executionId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/executions/{id} - Update execution status")
    class PatchExecutionOrderTests {

        @Test
        @DisplayName("Should update status to IN_REPAIR and return 200 OK")
        void shouldUpdateStatus() throws Exception {
            ExecutionOrder inRepairOrder = new ExecutionOrder(
                    executionId,
                    workOrderId,
                    "TECH-101",
                    ExecutionStatus.IN_REPAIR,
                    "Starting disassembly",
                    sampleOrder.getChecklist(),
                    sampleOrder.getMaterials(),
                    sampleOrder.getCreatedAt(),
                    Instant.now(),
                    null
            );

            when(updateExecutionStatusUseCase.updateStatus(eq(executionId), eq(ExecutionStatus.IN_REPAIR), anyString()))
                    .thenReturn(inRepairOrder);

            UpdateExecutionStatusRequest request = new UpdateExecutionStatusRequest()
                    .status(UpdateExecutionStatusRequest.StatusEnum.IN_REPAIR)
                    .notes("Starting disassembly");

            mockMvc.perform(patch("/api/v1/executions/{id}", executionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_REPAIR"));
        }

        @Test
        @DisplayName("Should update status to COMPLETED and return 200 with completedAt timestamp")
        void shouldCompleteOrder() throws Exception {
            Instant completedAt = Instant.now();
            ExecutionOrder completedOrder = new ExecutionOrder(
                    executionId,
                    workOrderId,
                    "TECH-101",
                    ExecutionStatus.COMPLETED,
                    "Work finished",
                    sampleOrder.getChecklist(),
                    sampleOrder.getMaterials(),
                    sampleOrder.getCreatedAt(),
                    completedAt,
                    completedAt
            );

            when(updateExecutionStatusUseCase.updateStatus(eq(executionId), eq(ExecutionStatus.COMPLETED), anyString()))
                    .thenReturn(completedOrder);

            UpdateExecutionStatusRequest request = new UpdateExecutionStatusRequest()
                    .status(UpdateExecutionStatusRequest.StatusEnum.COMPLETED)
                    .notes("Work finished");

            mockMvc.perform(patch("/api/v1/executions/{id}", executionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.completedAt").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/executions/{id}/checklist - Add checklist item")
    class AddChecklistItemTests {

        @Test
        @DisplayName("Should record checklist item and return 200 OK")
        void shouldAddChecklistItem() throws Exception {
            when(addChecklistItemUseCase.addChecklistItem(eq(executionId), anyString(), anyBoolean()))
                    .thenReturn(sampleOrder);

            AddChecklistItemRequest request = new AddChecklistItemRequest()
                    .task("Inspect brake fluid")
                    .completed(true);

            mockMvc.perform(post("/api/v1/executions/{id}/checklist", executionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.checklist").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/executions/{id}/materials - Add maintenance material")
    class AddMaintenanceMaterialTests {

        @Test
        @DisplayName("Should record maintenance material with materialCode and return 200 OK")
        void shouldAddMaterialWithMaterialCode() throws Exception {
            when(addMaterialUseCase.addMaterial(eq(executionId), eq("PART-BRK-01"), eq("Brake Pad"), eq(2)))
                    .thenReturn(sampleOrder);

            AddMaterialRequest request = new AddMaterialRequest(2)
                    .materialCode("PART-BRK-01")
                    .description("Brake Pad");

            mockMvc.perform(post("/api/v1/executions/{id}/materials", executionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.materials").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/executions/queue - List execution queue")
    class GetExecutionQueueTests {

        @Test
        @DisplayName("Should return paged execution queue")
        void shouldReturnExecutionQueue() throws Exception {
            when(listExecutionQueueUseCase.getQueue()).thenReturn(List.of(sampleOrder));

            mockMvc.perform(get("/api/v1/executions/queue")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
