package br.com.fiap.phase4.exec.iandt;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Execution Controller REST Integration Tests")
class ExecutionControllerIntegrationTest extends ExecIntegrationTest {

    @Test
    @DisplayName("Should execute full maintenance lifecycle from queue to technical completion (E2E Scenario 1)")
    void shouldExecuteFullMaintenanceLifecycleFromQueueToCompletion() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> createRequest = Map.of(
                "workOrderId", workOrderId.toString(),
                "technicianId", "TECH-101",
                "notes", "Initial maintenance queue entry"
        );

        // 1. Enqueue
        String executionId = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
        .when()
                .post("/api/v1/executions")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("workOrderId", equalTo(workOrderId.toString()))
                .body("status", equalTo("QUEUED"))
                .body("technicianId", equalTo("TECH-101"))
                .extract().path("id");

        // 2. Start Repair
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "IN_REPAIR", "notes", "Repair in progress"))
        .when()
                .patch("/api/v1/executions/{id}", executionId)
        .then()
                .statusCode(200)
                .body("id", equalTo(executionId))
                .body("status", equalTo("IN_REPAIR"));

        // 3. Complete Repair
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "COMPLETED", "notes", "Technical repair successfully finished"))
        .when()
                .patch("/api/v1/executions/{id}", executionId)
        .then()
                .statusCode(200)
                .body("id", equalTo(executionId))
                .body("status", equalTo("COMPLETED"))
                .body("completedAt", notNullValue());
    }

    @Test
    @DisplayName("Should retrieve active operational execution queue with pagination (E2E Scenario 2)")
    void shouldRetrieveExecutionQueueWithPagination() {
        UUID workOrder1 = UUID.randomUUID();
        UUID workOrder2 = UUID.randomUUID();

        given().contentType(ContentType.JSON)
                .body(Map.of("workOrderId", workOrder1.toString(), "technicianId", "TECH-101"))
                .post("/api/v1/executions");

        given().contentType(ContentType.JSON)
                .body(Map.of("workOrderId", workOrder2.toString(), "technicianId", "TECH-102"))
                .post("/api/v1/executions");

        given()
                .contentType(ContentType.JSON)
        .when()
                .get("/api/v1/executions/queue")
        .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("totalElements", greaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("Should record technical impediment and transition execution order to FAILED (E2E Scenario 3)")
    void shouldRecordTechnicalImpedimentAndTransitionToFailed() {
        UUID workOrderId = UUID.randomUUID();
        String executionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("workOrderId", workOrderId.toString(), "technicianId", "TECH-101"))
                .post("/api/v1/executions")
                .then().statusCode(201).extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "IN_REPAIR"))
                .patch("/api/v1/executions/{id}", executionId);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "FAILED", "notes", "Impossibilidade de reparo: bloco do motor trincado"))
        .when()
                .patch("/api/v1/executions/{id}", executionId)
        .then()
                .statusCode(200)
                .body("status", equalTo("FAILED"))
                .body("notes", containsString("bloco do motor trincado"))
                .body("completedAt", notNullValue());
    }

    @Test
    @DisplayName("Should reject status change on already finalized execution order with HTTP 400 (E2E Scenario 4)")
    void shouldRejectStatusUpdateOnAlreadyCompletedExecution() {
        UUID workOrderId = UUID.randomUUID();
        String executionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("workOrderId", workOrderId.toString(), "technicianId", "TECH-101"))
                .post("/api/v1/executions")
                .then().statusCode(201).extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "COMPLETED"))
                .patch("/api/v1/executions/{id}", executionId)
                .then().statusCode(200);

        // Attempt to reopen completed order
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "IN_REPAIR"))
        .when()
                .patch("/api/v1/executions/{id}", executionId)
        .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should record maintenance materials and parts into execution document (Materials E2E Scenario 1)")
    void shouldAddMaintenanceMaterialsToExecution() {
        UUID workOrderId = UUID.randomUUID();
        String executionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("workOrderId", workOrderId.toString(), "technicianId", "TECH-101"))
                .post("/api/v1/executions")
                .then().statusCode(201).extract().path("id");

        Map<String, Object> materialRequest = Map.of(
                "materialCode", "PART-BRK-01",
                "description", "Pastilha de Freio Dianteira",
                "quantity", 2
        );

        given()
                .contentType(ContentType.JSON)
                .body(materialRequest)
        .when()
                .post("/api/v1/executions/{id}/materials", executionId)
        .then()
                .statusCode(200)
                .body("materials", hasSize(1))
                .body("materials[0].sku", equalTo("PART-BRK-01"))
                .body("materials[0].quantity", equalTo(2));
    }

    @Test
    @DisplayName("Should record inspection checklist items into execution document (Materials E2E Scenario 2)")
    void shouldAddChecklistItemsToExecution() {
        UUID workOrderId = UUID.randomUUID();
        String executionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("workOrderId", workOrderId.toString(), "technicianId", "TECH-101"))
                .post("/api/v1/executions")
                .then().statusCode(201).extract().path("id");

        Map<String, Object> checklistRequest = Map.of(
                "task", "Verificar fluido de freio",
                "completed", true
        );

        given()
                .contentType(ContentType.JSON)
                .body(checklistRequest)
        .when()
                .post("/api/v1/executions/{id}/checklist", executionId)
        .then()
                .statusCode(200)
                .body("checklist", hasSize(1))
                .body("checklist[0].task", equalTo("Verificar fluido de freio"))
                .body("checklist[0].completed", equalTo(true));
    }

    @Test
    @DisplayName("Should retrieve consolidated execution details containing materials and checklist (Materials E2E Scenario 3)")
    void shouldRetrieveConsolidatedExecutionOrderWithMaterialsAndChecklist() {
        UUID workOrderId = UUID.randomUUID();
        String executionId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("workOrderId", workOrderId.toString(), "technicianId", "TECH-101"))
                .post("/api/v1/executions")
                .then().statusCode(201).extract().path("id");

        // Add material
        given().contentType(ContentType.JSON)
                .body(Map.of("materialCode", "PART-FLT-02", "description", "Filtro de Oleo Motor", "quantity", 1))
                .post("/api/v1/executions/{id}/materials", executionId);

        // Add checklist item
        given().contentType(ContentType.JSON)
                .body(Map.of("task", "Substituicao do filtro de oleo", "completed", true))
                .post("/api/v1/executions/{id}/checklist", executionId);

        // Fetch consolidated details
        given()
                .contentType(ContentType.JSON)
        .when()
                .get("/api/v1/executions/{id}", executionId)
        .then()
                .statusCode(200)
                .body("id", equalTo(executionId))
                .body("materials", hasSize(1))
                .body("materials[0].sku", equalTo("PART-FLT-02"))
                .body("checklist", hasSize(1))
                .body("checklist[0].task", equalTo("Substituicao do filtro de oleo"));
    }

    @Test
    @DisplayName("Should return HTTP 404 when querying nonexistent execution order")
    void shouldReturn404WhenExecutionNotFound() {
        given()
                .contentType(ContentType.JSON)
        .when()
                .get("/api/v1/executions/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }
}
