package br.com.cnab.hub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaTopicsProperties(
        String receivedTopic,
        String rejectionTopic,
        long outboxPollMs
) {
}
