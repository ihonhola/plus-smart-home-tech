package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import java.util.concurrent.ExecutionException;


@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventSender {

    private final Producer<String, SpecificRecordBase> kafkaProducer;

    private static final String SENSORS_TOPIC = "telemetry.sensors.v1";
    private static final String HUBS_TOPIC = "telemetry.hubs.v1";

    public void sendSensorEvent(SensorEventAvro event) {
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(SENSORS_TOPIC, event.getHubId(), event);
        /*kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send sensor event: {}", event, exception);
            } else {
                log.debug("Sensor event sent: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });*/
        try {
        kafkaProducer.send(record).get(); // блокируемся до подтверждения записи
        log.debug("Sensor event sent: hubId={}, sensorId={}", event.getHubId(), event.getId());
        } catch (InterruptedException | ExecutionException e) {
        log.error("Failed to send sensor event: {}", event, e);
        throw new RuntimeException(e); // прерываем gRPC с ошибкой
        }
    }

    public void sendHubEvent(HubEventAvro event) {
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(HUBS_TOPIC, event.getHubId(), event);
        /*kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send hub event: {}", event, exception);
            } else {
                log.debug("Hub event sent: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });*/
        try {
            kafkaProducer.send(record).get();
            log.debug("Hub event sent: hubId={}, type={}", event.getHubId(), event.getPayload().getClass().getSimpleName());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to send hub event: {}", event, e);
            throw new RuntimeException(e);
        }
    }

    /*public void sendSensorEvent(SensorEvent event) {
        SensorEventAvro avroEvent = mapToAvro(event);
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(SENSORS_TOPIC, avroEvent.getHubId(), avroEvent);
        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send sensor event: {}", event, exception);
            } else {
                log.debug("Sensor event sent: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    public void sendHubEvent(HubEvent event) {
        HubEventAvro avroEvent = mapToAvro(event);
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(HUBS_TOPIC, avroEvent.getHubId(), avroEvent);
        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send hub event: {}", event, exception);
            } else {
                log.debug("Hub event sent: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    private SensorEventAvro mapToAvro(SensorEvent event) {
        Object payload = switch (event) {
            case ClimateSensorEvent e -> ClimateSensorAvro.newBuilder()
                    .setTemperatureC(e.getTemperatureC())
                    .setHumidity(e.getHumidity())
                    .setCo2Level(e.getCo2Level())
                    .build();
            case LightSensorEvent e -> LightSensorAvro.newBuilder()
                    .setLinkQuality(e.getLinkQuality())
                    .setLuminosity(e.getLuminosity())
                    .build();
            case MotionSensorEvent e -> MotionSensorAvro.newBuilder()
                    .setLinkQuality(e.getLinkQuality())
                    .setMotion(e.getMotion())
                    .setVoltage(e.getVoltage())
                    .build();
            case SwitchSensorEvent e -> SwitchSensorAvro.newBuilder()
                    .setState(e.getState())
                    .build();
            case TemperatureSensorEvent e -> TemperatureSensorAvro.newBuilder()
                    .setTemperatureC(e.getTemperatureC())
                    .setTemperatureF(e.getTemperatureF())
                    .build();
            default -> throw new IllegalArgumentException("Unknown sensor event type: " + event.getClass());
        };

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }

    private HubEventAvro mapToAvro(HubEvent event) {
        Object payload = switch (event) {
            case DeviceAddedEvent e -> DeviceAddedEventAvro.newBuilder()
                    .setId(e.getId())
                    .setType(DeviceTypeAvro.valueOf(e.getDeviceType().name()))
                    .build();
            case DeviceRemovedEvent e -> DeviceRemovedEventAvro.newBuilder()
                    .setId(e.getId())
                    .build();
            case ScenarioAddedEvent e -> {
                var conditions = e.getConditions().stream()
                        .map(c -> ScenarioConditionAvro.newBuilder()
                                .setSensorId(c.getSensorId())
                                .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                                .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()))
                                .setValue(c.getValue())
                                .build())
                        .toList();
                var actions = e.getActions().stream()
                        .map(a -> DeviceActionAvro.newBuilder()
                                .setSensorId(a.getSensorId())
                                .setType(ActionTypeAvro.valueOf(a.getType().name()))
                                .setValue(a.getValue())
                                .build())
                        .toList();
                yield ScenarioAddedEventAvro.newBuilder()
                        .setName(e.getName())
                        .setConditions(conditions)
                        .setActions(actions)
                        .build();
            }
            case ScenarioRemovedEvent e -> ScenarioRemovedEventAvro.newBuilder()
                    .setName(e.getName())
                    .build();
            default -> throw new IllegalArgumentException("Unknown hub event type: " + event.getClass());
        };

        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }*/
}