# Plano de Implementação: Módulo de Testes de Integração (iandt) no api-exec

> **Decisão do Usuário**: Opção 1 - Criação do Submódulo Maven Dedicado iandt (exec-iandt) no 15soat-phase4-api-exec
> **Status**: Aprovado para Execução

---

## 1. Objetivo
Implementar o módulo físico de testes de integração `exec-iandt` (`iandt`) no microsserviço `15soat-phase4-api-exec`, baseado na arquitetura da Phase 3 e nos padrões estabelecidos no `api-billing`, reutilizando as fixtures de Testcontainers (MongoDB e Kafka) do `mod-integration-test` do `15soat-phase4-lib-commons` e cobrindo os cenários das features E2E da oficina (`execution_queue_and_repair.feature` e `materials_and_checklist.feature`).

---

## 2. Escopo Técnico da Implementação

### 2.1. Configuração Maven do Parent (`15soat-phase4-api-exec/pom.xml`)
- Adicionar o submódulo `<module>iandt</module>` à lista de `<modules>`.
- Adicionar os artefatos internos `exec-application`, `exec-domain` e `exec-iandt` no `<dependencyManagement>`.
- Adicionar `mod-integration-test` no `<dependencyManagement>`.

### 2.2. Criação do Módulo `15soat-phase4-api-exec/iandt`
- Criar a estrutura de diretórios `iandt/src/test/java` e `iandt/src/test/resources`.
- Criar o arquivo `iandt/pom.xml`:
  - Dependência em `exec-application` e `exec-domain`.
  - Dependência em `mod-integration-test` (`<scope>test</scope>`).
  - Dependência em `spring-boot-starter-test` (`<scope>test</scope>`).
  - Dependência em `org.awaitility:awaitility` para asserções assíncronas do Kafka.
  - Perfil Maven `integrationTests` (`-DintegrationTests=true`) para ativação sob demanda (com `<skipTests>true</skipTests>` por padrão para Graceful Degradation).

### 2.3. Arquivo de Configuração de Teste (`iandt/src/test/resources/application-test.yml`)
- Configuração do contexto Spring para os testes integrados:
  - `spring.application.name: exec-iandt`
  - `spring.data.mongodb.auto-index-creation: true`

### 2.4. Implementação da Classe Base (`ExecIntegrationTest.java`)
- Pacote: `br.com.fiap.phase4.exec.iandt`
- Anotações:
  - `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ExecApplication.class)`
  - `@ActiveProfiles("test")`
- Herança de Interfaces do Commons:
  - `implements MongoSetup, KafkaSetup`
- Injeções de Infraestrutura:
  - `@LocalServerPort private Integer port;`
  - `SpringDataExecutionOrderRepository executionOrderRepository;`
  - `KafkaTemplate<String, Object> kafkaTemplate;`
  - `ConsumerFactory<String, Object> consumerFactory;`
- Ciclo de Vida `@BeforeEach`:
  - Configuração do RestAssured: `RestAssured.baseURI = String.format("http://localhost:%s", port);`
  - Limpeza determinística das coleções do MongoDB (`deleteAll()`).

### 2.5. Suítes de Testes de Integração Concretas
1. **`ExecutionPersistenceIntegrationTest.java`**:
   - Validação da persistência real de documentos no MongoDB (`ExecutionOrderMongoDocument`).
   - Validação de listas embutidas (`ChecklistItemDocument` e `MaintenanceMaterialDocument`).
   - Validação de queries de fila e busca por `workOrderId`.
2. **`ExecutionControllerIntegrationTest.java` (Cenários E2E Oficina)**:
   - *Caminho Feliz (Cenário 1 E2E)*: Enfileiramento `QUEUED`, início `IN_REPAIR`, conclusão `COMPLETED` com registro de `completedAt`.
   - *Fila Operacional (Cenário 2 E2E)*: `GET /api/v1/executions/queue` com paginação.
   - *Impedimento Técnico (Cenário 3 E2E)*: Transição para `FAILED` com notas justificativas.
   - *Imutabilidade Pós-Finalização (Cenário 4 E2E)*: Rejeição com HTTP 400 ao tentar atualizar execução `COMPLETED`.
   - *Gestão de Materiais (Cenário 1 e 3 Materials E2E)*: `POST /api/v1/executions/{id}/materials` vinculando peças e insumos.
   - *Checklist de Inspeção (Cenário 2 Materials E2E)*: `POST /api/v1/executions/{id}/checklist` registrando tarefas de inspeção.
   - *Consulta Consolidada*: `GET /api/v1/executions/{id}` validando o agregado documental completo no MongoDB.
3. **`ExecEventsKafkaIntegrationTest.java` (Cenários Saga e Mensageria)**:
   - *Inbound*: Consumo de `PaymentConfirmedEvent` no tópico `payment-events` e confirmação com Awaitility de que a ordem foi criada no MongoDB com status `QUEUED`.
   - *Outbound*: Verificação com consumidor de teste de que o evento `ExecutionStartedEvent` é publicado no tópico `execution-events` quando o reparo é iniciado, e `ExecutionCompletedEvent` quando finalizado.

### 2.6. Quality Gate e Validação
- Compilação completa com `mvn clean test-compile` no reactor do `15soat-phase4-api-exec`.
- Execução de testes de unidade sem interferência via `mvn test`.
- Código e assertivas estritamente em inglês.
