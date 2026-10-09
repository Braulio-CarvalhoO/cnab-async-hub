package br.com.cnab.hub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({StorageProperties.class, KafkaTopicsProperties.class})
public class AppPropertiesConfig {
}
