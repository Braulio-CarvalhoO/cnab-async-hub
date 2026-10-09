package br.com.cnab.hub.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProcessamentoRepository extends MongoRepository<ProcessamentoDocument, String> {

    Optional<ProcessamentoDocument> findByCorrelationId(String correlationId);

    Optional<ProcessamentoDocument> findByFileHash(String fileHash);

    List<ProcessamentoDocument> findTop20ByOrderByCreatedAtDesc();
}
