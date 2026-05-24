package ru.yandex.practicum.aggregator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties("aggregator.kafka")
public class KafkaProperties {
    private Consumer consumer = new Consumer();
    private Producer producer = new Producer();

    @Getter @Setter
    public static class Consumer {
        private String bootstrapServers;
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
    }

    @Getter @Setter
    public static class Producer {
        private String bootstrapServers;
        private String keySerializer;
        private String valueSerializer;
    }
}