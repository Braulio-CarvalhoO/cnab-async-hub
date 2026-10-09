package br.com.cnab.hub.ingest;

import br.com.cnab.hub.config.KafkaTopicsProperties;
import br.com.cnab.hub.config.StorageProperties;
import br.com.cnab.hub.domain.IngestSource;
import br.com.cnab.hub.domain.ProcessamentoStatus;
import br.com.cnab.hub.kafka.CnabReceivedEvent;
import br.com.cnab.hub.persistence.OutboxEventDocument;
import br.com.cnab.hub.persistence.OutboxEventRepository;
import br.com.cnab.hub.persistence.ProcessamentoDocument;
import br.com.cnab.hub.persistence.ProcessamentoRepository;
import br.com.cnab.hub.storage.ObjectStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RemessaIngestService {

    private final ObjectStorage objectStorage;
    private final StorageProperties storageProperties;
    private final KafkaTopicsProperties kafkaProperties;
    private final ProcessamentoRepository processamentoRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public RemessaIngestService(
            ObjectStorage objectStorage,
            StorageProperties storageProperties,
            KafkaTopicsProperties kafkaProperties,
            ProcessamentoRepository processamentoRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.objectStorage = objectStorage;
        this.storageProperties = storageProperties;
        this.kafkaProperties = kafkaProperties;
        this.processamentoRepository = processamentoRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public ProcessamentoDocument ingest(
            byte[] content,
            String filename,
            IngestSource source,
            String correlationId,
            String forcedFailure
    ) {
        String hash = sha256(content);
        return processamentoRepository.findByFileHash(hash).orElseGet(() -> {
            String id = (correlationId == null || correlationId.isBlank())
                    ? UUID.randomUUID().toString()
                    : correlationId;
            String key = "incoming/%s/%s".formatted(id, filename == null ? "remessa.rem" : filename);
            objectStorage.upload(storageProperties.inputBucket(), key, content, "text/plain");

            Instant now = Instant.now();
            ProcessamentoDocument doc = new ProcessamentoDocument();
            doc.setCorrelationId(id);
            doc.setFileHash(hash);
            doc.setSource(source);
            doc.setStatus(ProcessamentoStatus.RECEIVED);
            doc.setInputKey(key);
            doc.setCreatedAt(now);
            doc.setUpdatedAt(now);
            processamentoRepository.save(doc);

            CnabReceivedEvent event = new CnabReceivedEvent(
                    UUID.randomUUID().toString(),
                    id,
                    source,
                    storageProperties.inputBucket(),
                    key,
                    content.length,
                    hash,
                    now,
                    forcedFailure
            );
            OutboxEventDocument outbox = new OutboxEventDocument();
            outbox.setTopic(kafkaProperties.receivedTopic());
            outbox.setPartitionKey(id);
            outbox.setPayload(toJson(event));
            outbox.setCreatedAt(now);
            outboxEventRepository.save(outbox);
            return doc;
        });
    }

    private String toJson(CnabReceivedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar evento Kafka", e);
        }
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
