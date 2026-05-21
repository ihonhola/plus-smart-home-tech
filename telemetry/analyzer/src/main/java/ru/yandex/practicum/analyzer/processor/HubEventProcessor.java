package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaConsumerProperties;
import ru.yandex.practicum.analyzer.service.ScenarioService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {
    private final KafkaConsumerProperties properties;
    private final ScenarioService scenarioService;
    private Consumer<String, HubEventAvro> consumer;

    @Override
    public void run() {
        try {
            // настройка консьюмера
            var config = properties.getHubConsumer();
            Properties props = new Properties();
            props.put("bootstrap.servers", config.getBootstrapServers());
            props.put("group.id", config.getGroupId());
            props.put("key.deserializer", config.getKeyDeserializer());
            props.put("value.deserializer", config.getValueDeserializer());
            // можно добавить max.poll.records и т.д.
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(List.of("telemetry.hubs.v1"));

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    try {
                        scenarioService.processHubEvent(record.value());
                    } catch (Exception e) {
                        log.error("Ошибка обработки события хаба", e);
                    }
                }
                consumer.commitSync();
            }
        } catch (Exception e) {
            log.error("Критическая ошибка в HubEventProcessor", e);
        } finally {
            if (consumer != null) {
                consumer.close();
                log.info("Консьюмер HubEventProcessor закрыт");
            }
        }
    }

    // Метод для корректной остановки (можно вызывать из shutdown hook)
    public void shutdown() {
        if (consumer != null) {
            consumer.wakeup();
        }
    }
}