# Plano de Implementação: Camada de Aplicação e Adaptadores de Execução de Oficina

> **Decisão do Usuário**: Opção 1 - Centralização de Contratos, Inclusão de `/materials`, Configuração dos Plugins e Geração dos Fontes em `target`
> **Status**: Concluído com Sucesso (100% dos testes aprovados)

## Contexto e Objetivo
Implementar a camada de aplicação (`exec-application`) do microsserviço `15soat-phase4-api-exec`, garantindo total simetria com os padrões estabelecidos em `15soat-phase4-api-work-order`:
1. Sincronizar os contratos `openapi.yaml` e `asyncapi.yaml` para `src/main/resources/contracts/`.
2. Adicionar no `openapi.yaml` a especificação do endpoint `POST /api/v1/executions/{id}/materials` exigido pelos cenários Gherkin e diagramas de sequência de E2E.
3. Configurar os plugins Maven `openapi-generator-maven-plugin` e `zenwave-sdk-maven-plugin` no `application/pom.xml` para gerar código contratual em `target/generated-sources/`.
4. Implementar os adaptadores Web (`ExecutionController`, `ExecutionExceptionHandler`).
5. Implementar os adaptadores de persistência NoSQL MongoDB (`ExecutionOrderMongoDocument`, `SpringDataExecutionOrderRepository`, `ExecutionRepositoryAdapter`).
6. Implementar os adaptadores de mensageria Kafka (`ExecKafkaPublisherAdapter`, `ExecKafkaConsumer`).
7. Calibrar o Helm Chart (`values.yaml`, `deployment.yaml`) para o cluster EKS.

## Etapas de Execução

### 1. Contratos em `resources`
- Copiar `contracts/openapi.yaml` e `contracts/asyncapi.yaml` para `application/src/main/resources/contracts/`.
- Declarar o path `/api/v1/executions/{id}/materials` no `openapi.yaml` com request body `AddMaterialRequest` (compatível com `sku`/`materialCode`, `name`/`description` e `quantity`) retornando `ExecutionDetailedResponse`.

### 2. Configuração de Plugins no `application/pom.xml`
- Ativar `openapi-generator-maven-plugin` gerando `br.com.fiap.phase4.exec.adapter.in.web.api` e DTOs em `br.com.fiap.phase4.exec.adapter.in.web.dto`.
- Adicionar `zenwave-sdk-maven-plugin` gerando DTOs em `br.com.fiap.phase4.exec.adapter.in.kafka.dto`.
- Configurar `build-helper-maven-plugin` adicionando os diretórios gerados.
- Executar `mvn compile` para validar a geração dos fontes.

### 3. Adaptadores Web & Exception Handler
- `ExecutionController`: Atender todos os endpoints da interface `ExecutionApi`.
- `ExecutionExceptionHandler`: Tratar `DomainException` e `ResourceNotFoundException` gerando Problem Details RFC 7807 com códigos semânticos.

### 4. Adaptadores MongoDB
- `ExecutionOrderMongoDocument`: Coleção `execution_orders` com subdocumentos `checklist` e `materials`.
- `SpringDataExecutionOrderRepository`: Consultas por `workOrderId` e `statusIn`.
- `ExecutionRepositoryAdapter`: Mapeamento entre o agregado `ExecutionOrder` e o documento MongoDB.

### 5. Adaptadores Kafka
- `ExecKafkaPublisherAdapter`: Emissão de eventos `execution-events`.
- `ExecKafkaConsumer`: Consumo de `payment-events` (`PaymentConfirmed`).
