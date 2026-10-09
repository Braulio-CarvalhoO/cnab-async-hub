package br.com.cnab.hub.processing;

import br.com.cnab.hub.cnab.Cnab240ParseStrategy;
import br.com.cnab.hub.cnab.Cnab400ParseStrategy;
import br.com.cnab.hub.cnab.CnabFileFactory;
import br.com.cnab.hub.cnab.CnabParseStrategyResolver;
import br.com.cnab.hub.cnab.RemessaValidator;
import br.com.cnab.hub.cnab.RetornoGenerator;
import br.com.cnab.hub.config.KafkaTopicsProperties;
import br.com.cnab.hub.config.StorageProperties;
import br.com.cnab.hub.domain.IngestSource;
import br.com.cnab.hub.domain.ProcessamentoStatus;
import br.com.cnab.hub.kafka.CnabReceivedEvent;
import br.com.cnab.hub.persistence.ProcessamentoDocument;
import br.com.cnab.hub.persistence.ProcessamentoRepository;
import br.com.cnab.hub.retry.RetryableOperations;
import br.com.cnab.hub.storage.ObjectStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemessaProcessorTest {

    @Mock
    private ObjectStorage objectStorage;
    @Mock
    private ProcessamentoRepository processamentoRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private RetryableOperations retryableOperations;

    private RemessaProcessor processor;
    private ProcessamentoDocument document;

    @BeforeEach
    void setUp() {
        processor = new RemessaProcessor(
                objectStorage,
                new StorageProperties("http://localhost:9000", "us-east-1", "a", "s", "cnab-input", "cnab-output"),
                new KafkaTopicsProperties("cnab-received", "cnab-rejection-dlq", 2000),
                new CnabParseStrategyResolver(List.of(new Cnab240ParseStrategy(), new Cnab400ParseStrategy())),
                new RemessaValidator(),
                new RetornoGenerator(),
                retryableOperations,
                processamentoRepository,
                kafkaTemplate,
                new ObjectMapper(),
                new SimpleMeterRegistry()
        );
        document = new ProcessamentoDocument();
        document.setCorrelationId("cid-1");
        document.setStatus(ProcessamentoStatus.RECEIVED);
        when(processamentoRepository.findByCorrelationId("cid-1")).thenReturn(Optional.of(document));
        when(processamentoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void processaArquivoValidoAteEntrega() {
        byte[] remessa = CnabFileFactory.cnab240(
                "237", "EMPRESA DEMO LTDA", "12345678000190",
                List.of(new CnabFileFactory.Item("FORNECEDOR ALFA", "NF1", 50_000))
        );
        when(objectStorage.download("cnab-input", "incoming/cid-1/file.rem")).thenReturn(remessa);
        when(retryableOperations.persist(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(retryableOperations.generate(any(), any())).thenAnswer(invocation -> {
            Supplier<byte[]> generator = invocation.getArgument(0);
            return generator.get();
        });

        processor.process(event());

        assertThat(document.getStatus()).isEqualTo(ProcessamentoStatus.DELIVERED);
        assertThat(document.getLayout().name()).isEqualTo("CNAB_240");
        assertThat(document.getOutputKey()).isEqualTo("retorno/cid-1/retorno.ret");
        verify(retryableOperations).postToS3(eq("cnab-output"), eq("retorno/cid-1/retorno.ret"), any(), anyString(), any());
    }

    @Test
    void rejeitaArquivoInvalidoEPublicaDlqDeNegocio() {
        byte[] remessa = CnabFileFactory.cnab240(
                "237", "EMPRESA DEMO LTDA", "12345678000190",
                List.of(new CnabFileFactory.Item("FORNECEDOR ALFA", "NF1", 0))
        );
        when(objectStorage.download("cnab-input", "incoming/cid-1/file.rem")).thenReturn(remessa);

        processor.process(event());

        assertThat(document.getStatus()).isEqualTo(ProcessamentoStatus.REJECTED);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("cnab-rejection-dlq"), eq("cid-1"), payload.capture());
        assertThat(payload.getValue()).contains("VALIDATION");
    }

    private CnabReceivedEvent event() {
        return new CnabReceivedEvent(
                "evt-1",
                "cid-1",
                IngestSource.API,
                "cnab-input",
                "incoming/cid-1/file.rem",
                10,
                "hash",
                Instant.now(),
                null
        );
    }
}
