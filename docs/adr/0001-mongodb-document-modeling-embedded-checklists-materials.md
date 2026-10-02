# ADR 0001: Modelagem NoSQL Orientada a Documentos com Coleções Embutidas no MongoDB

* **Status**: Aceito (Accepted)
* **Data**: 2026-10-02
* **Escopo**: Específico do Microsserviço `15soat-phase4-api-exec`
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em Engenharia NoSQL e Arquitetura de Software

---

## 1. Contexto e Declaração do Problema

O microsserviço `15soat-phase4-api-exec` gerencia a execução do trabalho físico na oficina mecânica:
* Recepção na esteira de triagem após pagamento confirmado.
* Alocação de mecânico responsável.
* Realização de inspeções técnicas dinâmicas (checklists).
* Registro de peças e insumos consumidos na baia de manutenção.
* Conclusão e liberação do veículo.

Em um banco relacional rígido, cada checklist ou inspeção com tarefas e status exigiria normalização em múltiplas tabelas com foreign keys, gerando overhead desnecessário de joins para leitura e gravação da ficha da baia.

---

## 2. Decisão Arquitetural

Decidimos utilizar o **MongoDB / AWS DocumentDB** com uma **modelagem orientada a documentos agregados embutidos (*Embedded Documents Pattern*)**:

1. **Documento Raiz (Aggregate Root - Coleção `execution_orders`)**:
   - `_id`: ObjectId nativo do MongoDB.
   - `workOrderId`: String (identificador lógico da OS externa correlacionada).
   - `status`: String (`QUEUED`, `IN_REPAIR`, `COMPLETED`).
   - `technicianId`: Identificador do mecânico alocado.
   - `startedAt` e `finishedAt`: Timestamps operacionais.

2. **Subdocumentos Embutidos (Embedded Arrays)**:
   - `checklists`: Array de subdocumentos com `[ { "task": "Freios", "completed": true }, ... ]`.
   - `materials`: Array de insumos com `[ { "sku": "PART-OIL-01", "quantity": 4 }, ... ]`.

3. **Operações Atômicas em Nível de Documento**:
   - Atualizações no checklist e inclusão de materiais ocorrem via atualizações atômicas no documento da OS (`$push`, `$set`), eliminando concorrência em múltiplas tabelas.

---

## 3. Consequências

### Positivas:
* **Leitura e Gravação em Passo Único**: A tela/totem do mecânico obtém todas as instruções, itens de verificação e peças em uma única leitura de documento JSON.
* **Esquema Flexível**: Novos tipos de checagem técnica podem ser adicionados ao array `checklists` sem necessidade de migrations DDL no banco.
* **Alta Performance e Escalabilidade**: O particionamento nativo do DocumentDB por `workOrderId` permite escalar horizontalmente a esteira de oficina.

### Negativas / Trade-offs:
* **Desnormalização de Dados de Catálogo**: O SKU e nome da peça são gravados como dados operacionais daquela execução; relatórios agregados entre oficinas exigem aggregation pipelines do MongoDB.
