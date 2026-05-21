package ru.yandex.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.config.KafkaProperties;
import ru.yandex.practicum.aggregator.service.SnapshotAggregator;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final SnapshotAggregator snapshotAggregator;
    private final KafkaProperties kafkaProperties;
    private Consumer<String, SensorEventAvro> consumer;
    private Producer<String, SpecificRecordBase> producer;

    public void start() {
        try {
            initConsumer();
            initProducer();
            consumer.subscribe(List.of("telemetry.sensors.v1"));

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();
                    log.debug("Получено событие датчика: hubId={}, sensorId={}", event.getHubId(), event.getId());
                    Optional<SensorsSnapshotAvro> updated = snapshotAggregator.updateState(event);
                    updated.ifPresent(snapshot -> {
                        ProducerRecord<String, SpecificRecordBase> producerRecord =
                                new ProducerRecord<>("telemetry.snapshots.v1", snapshot.getHubId(), snapshot);
                        producer.send(producerRecord, (metadata, ex) -> {
                            if (ex != null) {
                                log.error("Ошибка при отправке снапшота: {}", snapshot, ex);
                            } else {
                                log.debug("Снапшот отправлен в топик: {}", metadata.topic());
                            }
                        });
                    });
                }
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка в цикле агрегации", e);
        } finally {
            closeResources();
        }
    }

    private void initConsumer() {
        Properties props = new Properties();
        KafkaProperties.Consumer c = kafkaProperties.getConsumer();
        props.put("bootstrap.servers", c.getBootstrapServers());
        props.put("group.id", c.getGroupId());
        props.put("key.deserializer", c.getKeyDeserializer());
        props.put("value.deserializer", c.getValueDeserializer());
        consumer = new KafkaConsumer<>(props);
    }

    private void initProducer() {
        Properties props = new Properties();
        KafkaProperties.Producer p = kafkaProperties.getProducer();
        props.put("bootstrap.servers", p.getBootstrapServers());
        props.put("key.serializer", p.getKeySerializer());
        props.put("value.serializer", p.getValueSerializer());
        producer = new KafkaProducer<>(props);
    }

    private void closeResources() {
        try {
            if (producer != null) {
                producer.flush();
                producer.close();
            }
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }
}