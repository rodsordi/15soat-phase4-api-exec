package br.com.fiap.phase4.exec.adapter.out.persistence.repository;

import br.com.fiap.phase4.exec.adapter.out.persistence.entity.ExecutionOrderMongoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataExecutionOrderRepository extends MongoRepository<ExecutionOrderMongoDocument, String> {
    Optional<ExecutionOrderMongoDocument> findByWorkOrderId(String workOrderId);
    List<ExecutionOrderMongoDocument> findByStatusIn(List<String> statuses);
}
