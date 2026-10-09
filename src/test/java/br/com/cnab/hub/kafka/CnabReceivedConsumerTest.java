package br.com.cnab.hub.kafka;

import br.com.cnab.hub.domain.IngestSource;
import br.com.cnab.hub.processing.RemessaProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CnabReceivedConsumerTest {

    @Mock
    private RemessaProcessor remessaProcessor;

    @Test
    void desserializaEventoEDelegaAoProcessor() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        CnabReceivedConsumer consumer = new CnabReceivedConsumer(mapper, remessaProcessor);
        CnabReceivedEvent event = new CnabReceivedEvent(
                "e1", "c1", IngestSource.FRONTEND, "cnab-input", "key", 12, "hash", Instant.parse("2026-10-09T12:00:00Z"), null
        );

        consumer.onMessage(mapper.writeValueAsString(event));

        verify(remessaProcessor).process(eq(event));
    }
}
