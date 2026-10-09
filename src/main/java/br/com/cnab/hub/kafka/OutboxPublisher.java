package br.com.cnab.hub.kafka;

import br.com.cnab.hub.persistence.OutboxEventDocument;
import br.com.cnab.hub.persistence.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox-poll-ms:2000}")
    public void publishPending() {
        for (OutboxEventDocument event : repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Falha ao publicar outbox {}: {}", event.getId(), error.getMessage());
                            return;
                        }
                        event.setPublishedAt(Instant.now());
                        repository.save(event);
                        log.info("Evento outbox {} publicado em {}", event.getId(), event.getTopic());
                    });
        }
    }
}
