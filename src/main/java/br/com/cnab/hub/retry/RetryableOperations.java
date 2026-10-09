package br.com.cnab.hub.retry;

import br.com.cnab.hub.persistence.ProcessamentoDocument;
import br.com.cnab.hub.persistence.ProcessamentoRepository;
import br.com.cnab.hub.storage.ObjectStorage;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RetryableOperations {

    private static final Logger log = LoggerFactory.getLogger(RetryableOperations.class);
    private final ProcessamentoRepository processamentoRepository;
    private final ObjectStorage objectStorage;

    public RetryableOperations(ProcessamentoRepository processamentoRepository, ObjectStorage objectStorage) {
        this.processamentoRepository = processamentoRepository;
        this.objectStorage = objectStorage;
    }

    @Retry(name = "persistencia")
    public ProcessamentoDocument persist(ProcessamentoDocument document, String forcedFailure) {
        failIfRequested("persist", forcedFailure);
        log.info("Persistindo processamento {}", document.getCorrelationId());
        return processamentoRepository.save(document);
    }

    @Retry(name = "geracao")
    public byte[] generate(java.util.function.Supplier<byte[]> generator, String forcedFailure) {
        failIfRequested("geracao", forcedFailure);
        log.info("Gerando arquivo de retorno");
        return generator.get();
    }

    @Retry(name = "postagemS3")
    public void postToS3(String bucket, String key, byte[] content, String contentType, String forcedFailure) {
        failIfRequested("s3", forcedFailure);
        log.info("Postando {} em s3://{}/{}", key, bucket, key);
        objectStorage.upload(bucket, key, content, contentType);
    }

    private void failIfRequested(String expected, String forcedFailure) {
        if (expected.equalsIgnoreCase(forcedFailure)) {
            throw new IllegalStateException("Falha simulada em " + expected + " para demo de retry");
        }
    }
}
