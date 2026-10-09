package br.com.cnab.hub.ingest;

import br.com.cnab.hub.config.KafkaTopicsProperties;
import br.com.cnab.hub.config.StorageProperties;
import br.com.cnab.hub.domain.IngestSource;
import br.com.cnab.hub.domain.ProcessamentoStatus;
import br.com.cnab.hub.persistence.OutboxEventDocument;
import br.com.cnab.hub.persistence.OutboxEventRepository;
import br.com.cnab.hub.persistence.ProcessamentoDocument;
import br.com.cnab.hub.persistence.ProcessamentoRepository;
import br.com.cnab.hub.storage.ObjectStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemessaIngestServiceTest {

    @Mock
    private ObjectStorage objectStorage;
    @Mock
    private ProcessamentoRepository processamentoRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private RemessaIngestService service;

    @BeforeEach
    void setUp() {
        service = new RemessaIngestService(
                objectStorage,
                new StorageProperties("http://localhost:9000", "us-east-1", "a", "s", "cnab-input", "cnab-output"),
                new KafkaTopicsProperties("cnab-received", "cnab-rejection-dlq", 2000),
                processamentoRepository,
                outboxEventRepository,
                new ObjectMapper()
        );
    }

    @Test
    void gravaArquivoMongoEOutbox() {
        when(processamentoRepository.findByFileHash(any())).thenReturn(Optional.empty());
        when(processamentoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessamentoDocument doc = service.ingest("abc".getBytes(), "a.rem", IngestSource.API, "cid-9", null);

        assertThat(doc.getCorrelationId()).isEqualTo("cid-9");
        assertThat(doc.getStatus()).isEqualTo(ProcessamentoStatus.RECEIVED);
        verify(objectStorage).upload(org.mockito.ArgumentMatchers.eq("cnab-input"), org.mockito.ArgumentMatchers.contains("cid-9"), any(), any());
        ArgumentCaptor<OutboxEventDocument> outbox = ArgumentCaptor.forClass(OutboxEventDocument.class);
        verify(outboxEventRepository).save(outbox.capture());
        assertThat(outbox.getValue().getTopic()).isEqualTo("cnab-received");
        assertThat(outbox.getValue().getPayload()).contains("cid-9");
    }

    @Test
    void eIdempotenteParaOMesmoArquivo() {
        ProcessamentoDocument existing = new ProcessamentoDocument();
        existing.setCorrelationId("already");
        when(processamentoRepository.findByFileHash(any())).thenReturn(Optional.of(existing));

        ProcessamentoDocument result = service.ingest("abc".getBytes(), "a.rem", IngestSource.API, null, null);

        assertThat(result.getCorrelationId()).isEqualTo("already");
        verify(objectStorage, never()).upload(any(), any(), any(), any());
    }
}
