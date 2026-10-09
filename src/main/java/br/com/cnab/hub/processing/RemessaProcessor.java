package br.com.cnab.hub.processing;

import br.com.cnab.hub.cnab.CnabParseStrategy;
import br.com.cnab.hub.cnab.CnabParseStrategyResolver;
import br.com.cnab.hub.cnab.RemessaValidator;
import br.com.cnab.hub.cnab.RetornoGenerator;
import br.com.cnab.hub.cnab.model.ParsedRemessa;
import br.com.cnab.hub.config.KafkaTopicsProperties;
import br.com.cnab.hub.config.StorageProperties;
import br.com.cnab.hub.domain.ProcessamentoStatus;
import br.com.cnab.hub.kafka.CnabReceivedEvent;
import br.com.cnab.hub.kafka.CnabRejectionEvent;
import br.com.cnab.hub.persistence.ProcessamentoDocument;
import br.com.cnab.hub.persistence.ProcessamentoRepository;
import br.com.cnab.hub.retry.RetryableOperations;
import br.com.cnab.hub.storage.ObjectStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RemessaProcessor {

    private static final Logger log = LoggerFactory.getLogger(RemessaProcessor.class);

    private final ObjectStorage objectStorage;
    private final StorageProperties storageProperties;
    private final KafkaTopicsProperties kafkaProperties;
    private final CnabParseStrategyResolver strategyResolver;
    private final RemessaValidator validator;
    private final RetornoGenerator retornoGenerator;
    private final RetryableOperations retryableOperations;
    private final ProcessamentoRepository processamentoRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RemessaProcessor(
            ObjectStorage objectStorage,
            StorageProperties storageProperties,
            KafkaTopicsProperties kafkaProperties,
            CnabParseStrategyResolver strategyResolver,
            RemessaValidator validator,
            RetornoGenerator retornoGenerator,
            RetryableOperations retryableOperations,
            ProcessamentoRepository processamentoRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.objectStorage = objectStorage;
        this.storageProperties = storageProperties;
        this.kafkaProperties = kafkaProperties;
        this.strategyResolver = strategyResolver;
        this.validator = validator;
        this.retornoGenerator = retornoGenerator;
        this.retryableOperations = retryableOperations;
        this.processamentoRepository = processamentoRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public void process(CnabReceivedEvent event) {
        MDC.put("correlationId", event.correlationId());
        try {
            ProcessamentoDocument doc = processamentoRepository.findByCorrelationId(event.correlationId())
                    .orElseThrow(() -> new IllegalStateException("Processamento não encontrado: " + event.correlationId()));

            if (doc.getStatus() == ProcessamentoStatus.DELIVERED) {
                log.info("Idempotência: {} já entregue", event.correlationId());
                return;
            }

            byte[] content = objectStorage.download(event.bucket(), event.objectKey());
            update(doc, ProcessamentoStatus.DOWNLOADED, List.of());

            CnabParseStrategy strategy = strategyResolver.resolve(content);
            ParsedRemessa parsed = strategy.parse(content);
            doc.setLayout(parsed.layout());
            doc.setEmpresa(parsed.empresa());
            doc.setCnpj(parsed.cnpj());
            doc.setBanco(parsed.banco());
            doc.setValorTotalCentavos(parsed.valorTotalCentavos());
            doc.setQuantidadePagamentos(parsed.pagamentos().size());
            update(doc, ProcessamentoStatus.PARSED, List.of());

            List<String> errors = validator.validate(parsed);
            if (!errors.isEmpty()) {
                reject(doc, "VALIDATION", errors);
                return;
            }
            update(doc, ProcessamentoStatus.VALIDATED, List.of());

            retryableOperations.persist(doc, event.forcedFailure());
            update(doc, ProcessamentoStatus.PERSISTED, List.of());

            byte[] retorno = retryableOperations.generate(() -> retornoGenerator.generate(parsed), event.forcedFailure());
            update(doc, ProcessamentoStatus.RETORNO_GENERATED, List.of());

            String outputKey = "retorno/%s/retorno.ret".formatted(event.correlationId());
            retryableOperations.postToS3(
                    storageProperties.outputBucket(),
                    outputKey,
                    retorno,
                    "text/plain",
                    event.forcedFailure()
            );
            doc.setOutputKey(outputKey);
            update(doc, ProcessamentoStatus.DELIVERED, List.of());
            meterRegistry.counter("cnab.remessas.entregues").increment();
            log.info("Remessa {} entregue com layout {}", event.correlationId(), parsed.layout());
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void reject(ProcessamentoDocument doc, String reason, List<String> errors) {
        update(doc, ProcessamentoStatus.REJECTED, errors);
        meterRegistry.counter("cnab.remessas.rejeitadas", "reason", reason).increment();
        CnabRejectionEvent rejection = new CnabRejectionEvent(doc.getCorrelationId(), reason, errors, Instant.now());
        try {
            kafkaTemplate.send(
                    kafkaProperties.rejectionTopic(),
                    doc.getCorrelationId(),
                    objectMapper.writeValueAsString(rejection)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        log.warn("Remessa {} rejeitada: {}", doc.getCorrelationId(), errors);
    }

    private void update(ProcessamentoDocument doc, ProcessamentoStatus status, List<String> errors) {
        doc.setStatus(status);
        doc.setErrors(errors);
        doc.setUpdatedAt(Instant.now());
        processamentoRepository.save(doc);
    }
}
