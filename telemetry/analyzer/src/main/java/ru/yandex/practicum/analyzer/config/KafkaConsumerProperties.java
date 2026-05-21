package ru.yandex.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "analyzer.kafka")
@Component
public class KafkaConsumerProperties {
    private ConsumerConfig snapshotConsumer = new ConsumerConfig();
    private ConsumerConfig hubConsumer = new ConsumerConfig();

    @Getter @Setter
    public static class ConsumerConfig {
        private String bootstrapServers;
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
    }
}