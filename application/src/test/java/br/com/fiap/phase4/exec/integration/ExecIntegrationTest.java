package br.com.fiap.phase4.exec.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.fiap.phase4.exec.adapter.in.web.dto.CreateExecutionRequest;
import br.com.fiap.phase4.exec.adapter.in.web.dto.UpdateExecutionStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DisplayName("Execution Integration Tests (MongoDB + Kafka Testcontainers)")
class ExecIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0")
            .withReuse(true);

    @Container
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should create, retrieve and update execution order via REST with Testcontainers (MongoDB)")
    void shouldCreateRetrieveAndUpdateExecution() throws Exception {
        UUID workOrderId = UUID.randomUUID();
        var request = new CreateExecutionRequest();
        request.setWorkOrderId(workOrderId);
        request.setTechnicianId("TECH-INTEG");
        request.setNotes("Vehicle inspection test");

        String responseBody = mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(get("/api/v1/executions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.technicianId").value("TECH-INTEG"));

        var updateReq = new UpdateExecutionStatusRequest();
        updateReq.setStatus(UpdateExecutionStatusRequest.StatusEnum.DIAGNOSIS);
        updateReq.setNotes("Diagnostic started");

        mockMvc.perform(patch("/api/v1/executions/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DIAGNOSIS"));
    }
}
