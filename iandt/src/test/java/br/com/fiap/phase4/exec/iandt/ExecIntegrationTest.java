package br.com.fiap.phase4.exec.iandt;

import br.com.fiap.phase4.commons.integration.setup.KafkaSetup;
import br.com.fiap.phase4.commons.integration.setup.MongoSetup;
import br.com.fiap.phase4.exec.ExecApplication;
import br.com.fiap.phase4.exec.adapter.out.persistence.repository.SpringDataExecutionOrderRepository;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static java.lang.String.format;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = ExecApplication.class
)
public abstract class ExecIntegrationTest implements MongoSetup, KafkaSetup {

    @LocalServerPort
    protected Integer port;

    @Autowired
    protected SpringDataExecutionOrderRepository executionOrderRepository;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    protected ConsumerFactory<String, Object> consumerFactory;

    @BeforeEach
    void setUpBase() {
        RestAssured.baseURI = format("http://localhost:%s", port);

        log.info("Cleaning MongoDB execution_order collection before integration test execution");
        executionOrderRepository.deleteAll();
    }
}
