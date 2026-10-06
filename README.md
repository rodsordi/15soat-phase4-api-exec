# Microsserviço de Execução de Oficina (`15soat-phase4-api-exec`)

Microsserviço responsável pela gestão da fila física de execução da oficina mecânica, diagnósticos, reparos em baia e comunicação de conclusão na Saga coreografada via Apache Kafka.

## Arquitetura & Tecnologias
- **Java 25 LTS**
- **Spring Boot 4.0.7** & Spring Data MongoDB
- **Arquitetura Hexagonal (Ports & Adapters)**:
  - `domain`: Regras puras de negócio, sem dependência de frameworks.
  - `application`: Drivers Spring Boot, adaptadores de repositório de documentos MongoDB, consumidores e produtores de eventos Kafka, controllers REST.
- **Banco de Dados**: MongoDB (NoSQL) com armazenamento orientado a documentos para checklists dinâmicos, apontamentos de técnicos e ciclo de vida da execução.
- **Mensageria de Eventos**: Apache Kafka para coreografia distribuída da Saga.
- **Design API First**: OpenAPI 3.0 (`contracts/openapi.yaml`) & AsyncAPI (`contracts/asyncapi.yaml`).
- **Conteinerização & Deploy**: Build multi-stage com Docker, charts Helm para Kubernetes / Kong Ingress.

## Endpoints da API
- `POST /api/v1/executions` - Enfileira uma nova ordem de execução na oficina.
- `GET /api/v1/executions/queue` - Consulta a fila de execução ativa da oficina.
- `GET /api/v1/executions/{id}` - Obtém os detalhes completos de uma ordem de execução por ID.
- `PATCH /api/v1/executions/{id}/status` - Atualiza o status da execução física (`QUEUED`, `IN_REPAIR`, `COMPLETED`).
- `POST /api/v1/executions/{id}/checklist` - Adiciona ou atualiza um item de inspeção técnica no checklist.

## Eventos Kafka
- **Consome**: `payment-events` (O evento `PaymentConfirmedEvent` aciona a entrada automática na fila da oficina).
- **Publica**: `execution-events` (`ExecutionStartedEvent`, `ExecutionCompletedEvent`, `ExecutionFailedEvent`).

## Modelo de Dados: NoSQL Orientado a Documentos (MongoDB / AWS DocumentDB)

Diferente dos microsserviços relacionais, o **`api-exec`** utiliza o banco de dados orientado a documentos **MongoDB** (`exec_db`). 

Seguindo o padrão de **Agregados do DDD (Aggregate Root)**, a ordem de execução física (`execution_orders`) armazena atomicamente suas inspeções de checklist e peças aplicadas como **subdocumentos e arrays embutidos (Embedded Documents)**, dispensando tabelas normalizadas e joins relacionais.

Para visualizar o diagrama completo da estrutura de documentos, schemas e exemplos BSON/JSON, consulte a especificação do modelo de dados:

👉 **[MODEL.md - Diagrama do Modelo de Dados & Document Schema](MODEL.md)**

## Compilação & Testes
```bash
mvn clean test
```

## 🏛️ Architecture Decision Records (ADRs)

- **[Catálogo de ADRs do Microsserviço](docs/adr/README.md)**: Decisões específicas de Execução de Oficina (Modelagem Documental MongoDB com Checklists e Materiais Embutidos).
- **[Catálogo Global de ADRs (lib-commons)](../15soat-phase4-lib-commons/docs/adr/README.md)**: Padrões transversais compartilhados (Database-per-Service, Saga Kafka, Pragmatic DDD, Tabelas no Singular, Java 25 / Spring Boot 4).
