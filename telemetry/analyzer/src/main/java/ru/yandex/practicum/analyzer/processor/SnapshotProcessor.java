package ru.yandex.practicum.analyzer.processor;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.config.KafkaConsumerProperties;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.Condition;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {
    private final KafkaConsumerProperties properties;
    private final ScenarioRepository scenarioRepository;
    private Consumer<String, SensorsSnapshotAvro> consumer;

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public void start() {
        try {
            var config = properties.getSnapshotConsumer();
            Properties props = new Properties();
            props.put("bootstrap.servers", config.getBootstrapServers());
            props.put("group.id", config.getGroupId());
            props.put("key.deserializer", config.getKeyDeserializer());
            props.put("value.deserializer", config.getValueDeserializer());
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(List.of("telemetry.snapshots.v1"));

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    try {
                        processSnapshot(record.value());
                    } catch (Exception e) {
                        log.error("Ошибка обработки снапшота", e);
                    }
                }
                consumer.commitSync();
            }
        } catch (Exception e) {
            log.error("Критическая ошибка в SnapshotProcessor", e);
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    @Transactional
    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.debug("Получен снапшот для хаба {}", hubId);

        List<Scenario> scenarios = scenarioRepository.findByHubIdWithDetails(hubId);
        if (scenarios.isEmpty()) {
            return;
        }

        for (Scenario scenario : scenarios) {
            boolean allConditionsMet = checkScenarioConditions(scenario, snapshot);
            if (allConditionsMet) {
                executeScenarioActions(scenario, snapshot);
            }
        }
    }

    private boolean checkScenarioConditions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        for (ScenarioCondition sc : scenario.getScenarioConditions()) {
            String sensorId = sc.getSensor().getId();
            SensorStateAvro state = snapshot.getSensorsState().get(sensorId);
            if (state == null) {
                log.debug("Датчик {} отсутствует в снапшоте", sensorId);
                return false;
            }

            Condition condition = sc.getCondition();
            boolean conditionResult = evaluateCondition(condition, state.getData());
            if (!conditionResult) {
                log.debug("Условие не выполнено: датчик {}, тип={}", sensorId, condition.getType());
                return false;
            }
        }
        return true;
    }

    private boolean evaluateCondition(Condition condition, Object sensorData) {
        // Определяем значение датчика в зависимости от типа
        int sensorValue = extractSensorValue(condition.getType(), sensorData);
        int referenceValue = condition.getValue() != null ? condition.getValue() : 0;
        return switch (condition.getOperation()) {
            case "EQUALS" -> sensorValue == referenceValue;
            case "GREATER_THAN" -> sensorValue > referenceValue;
            case "LOWER_THAN" -> sensorValue < referenceValue;
            default -> throw new IllegalArgumentException("Неизвестная операция: " + condition.getOperation());
        };
    }

    private int extractSensorValue(String conditionType, Object data) {
        return switch (conditionType) {
            case "MOTION" -> {
                if (data instanceof MotionSensorAvro motion) yield motion.getMotion() ? 1 : 0;
                throw new IllegalStateException("Неверный тип данных для MOTION");
            }
            case "LUMINOSITY" -> {
                if (data instanceof LightSensorAvro light) yield light.getLuminosity();
                throw new IllegalStateException("Неверный тип данных для LUMINOSITY");
            }
            case "SWITCH" -> {
                if (data instanceof SwitchSensorAvro sw) yield sw.getState() ? 1 : 0;
                throw new IllegalStateException("Неверный тип данных для SWITCH");
            }
            case "TEMPERATURE" -> {
                if (data instanceof TemperatureSensorAvro temp) yield temp.getTemperatureC();
                throw new IllegalStateException("Неверный тип данных для TEMPERATURE");
            }
            case "CO2LEVEL" -> {
                if (data instanceof ClimateSensorAvro climate) yield climate.getCo2Level();
                throw new IllegalStateException("Неверный тип данных для CO2LEVEL");
            }
            case "HUMIDITY" -> {
                if (data instanceof ClimateSensorAvro climate) yield climate.getHumidity();
                throw new IllegalStateException("Неверный тип данных для HUMIDITY");
            }
            default -> throw new IllegalArgumentException("Неизвестный тип условия: " + conditionType);
        };
    }

    private void executeScenarioActions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        for (ScenarioAction sa : scenario.getScenarioActions()) {
            Action action = sa.getAction();
            DeviceActionProto protoAction = DeviceActionProto.newBuilder()
                    .setSensorId(sa.getSensor().getId())
                    .setType(ActionTypeProto.valueOf(action.getType()))
                    .setValue(action.getValue() != null ? action.getValue() : 0) // если null, ставим 0
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(snapshot.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(protoAction)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            try {
                hubRouterClient.handleDeviceAction(request);
                log.info("Отправлена команда: хаб={}, сценарий={}, действие={}",
                        snapshot.getHubId(), scenario.getName(), action.getType());
            } catch (StatusRuntimeException e) {
                log.error("Ошибка отправки команды в HubRouter", e);
            }
        }
    }
}