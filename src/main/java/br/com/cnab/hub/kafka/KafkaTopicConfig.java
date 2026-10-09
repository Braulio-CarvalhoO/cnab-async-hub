package br.com.cnab.hub.kafka;

import br.com.cnab.hub.config.KafkaTopicsProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic cnabReceivedTopic(KafkaTopicsProperties properties) {
        return TopicBuilder.name(properties.receivedTopic()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic cnabRejectionTopic(KafkaTopicsProperties properties) {
        return TopicBuilder.name(properties.rejectionTopic()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic cnabReceivedDlt(KafkaTopicsProperties properties) {
        return TopicBuilder.name(properties.receivedTopic() + ".DLT").partitions(3).replicas(1).build();
    }
}
