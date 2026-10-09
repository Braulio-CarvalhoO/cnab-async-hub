package br.com.cnab.hub.kafka;

import br.com.cnab.hub.processing.RemessaProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CnabReceivedConsumer {

    private static final Logger log = LoggerFactory.getLogger(CnabReceivedConsumer.class);
    private final ObjectMapper objectMapper;
    private final RemessaProcessor remessaProcessor;

    public CnabReceivedConsumer(ObjectMapper objectMapper, RemessaProcessor remessaProcessor) {
        this.objectMapper = objectMapper;
        this.remessaProcessor = remessaProcessor;
    }

    @KafkaListener(topics = "${app.kafka.received-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload) throws Exception {
        CnabReceivedEvent event = objectMapper.readValue(payload, CnabReceivedEvent.class);
        log.info("Consumindo evento {} da chave {}", event.eventId(), event.correlationId());
        remessaProcessor.process(event);
    }
}
