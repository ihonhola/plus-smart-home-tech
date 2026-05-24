package ru.yandex.practicum.collector.mapper;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ClimateSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceRemovedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioRemovedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SwitchSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.time.Instant;

@Component
public class ProtobufToAvroMapper {

    public SensorEventAvro mapSensorEvent(SensorEventProto event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(protoTimestampToInstant(event.getTimestamp()));

        switch (event.getPayloadCase()) {
            case MOTION_SENSOR -> {
                System.out.println("Получено событие датчика движения");
                MotionSensorProto m = event.getMotionSensor();
                System.out.println("Качество связи: " + m.getLinkQuality());
                System.out.println("Наличие движения: " + m.getMotion());
                System.out.println("Напряжение: " + m.getVoltage());
                builder.setPayload(
                        MotionSensorAvro.newBuilder()
                                .setLinkQuality(m.getLinkQuality())
                                .setMotion(m.getMotion())
                                .setVoltage(m.getVoltage())
                                .build()
                );
            }
            case TEMPERATURE_SENSOR -> {
                System.out.println("Получено событие датчика температуры");
                TemperatureSensorProto t = event.getTemperatureSensor();
                System.out.println("Температура в Цельсиях: " + t.getTemperatureC());
                System.out.println("Температура в Фаренгейтах: " + t.getTemperatureF());
                builder.setPayload(
                        TemperatureSensorAvro.newBuilder()
                                .setTemperatureC(t.getTemperatureC())
                                .setTemperatureF(t.getTemperatureF())
                                .build()
                );
            }
            case LIGHT_SENSOR -> {
                System.out.println("Получено событие датчика освещённости");
                LightSensorProto l = event.getLightSensor();
                System.out.println("Уровень освещённости: " + l.getLuminosity());
                System.out.println("Качество связи: " + l.getLinkQuality());
                builder.setPayload(
                        LightSensorAvro.newBuilder()
                                .setLinkQuality(l.getLinkQuality())
                                .setLuminosity(l.getLuminosity())
                                .build()
                );
            }
            case CLIMATE_SENSOR -> {
                System.out.println("Получено событие климатического датчика");
                ClimateSensorProto c = event.getClimateSensor();
                System.out.println("Температура: " + c.getTemperatureC());
                System.out.println("Влажность: " + c.getHumidity());
                System.out.println("Уровень CO2: " + c.getCo2Level());
                builder.setPayload(
                        ClimateSensorAvro.newBuilder()
                                .setTemperatureC(c.getTemperatureC())
                                .setHumidity(c.getHumidity())
                                .setCo2Level(c.getCo2Level())
                                .build()
                );
            }
            case SWITCH_SENSOR -> {
                System.out.println("Получено событие датчика переключателя");
                SwitchSensorProto s = event.getSwitchSensor();
                System.out.println("Состояние: " + (s.getState() ? "включён" : "выключен"));
                builder.setPayload(
                        SwitchSensorAvro.newBuilder()
                                .setState(s.getState())
                                .build()
                );
            }
            default -> throw new IllegalArgumentException("Unknown sensor payload: " + event.getPayloadCase());
        }
        return builder.build();
    }

    public HubEventAvro mapHubEvent(HubEventProto event) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(protoTimestampToInstant(event.getTimestamp()));

        switch (event.getPayloadCase()) {
            case DEVICE_ADDED -> {
                System.out.println("Получено событие добавления устройства");
                DeviceAddedEventProto d = event.getDeviceAdded();
                System.out.println("Идентификатор устройства: " + d.getId());
                System.out.println("Тип устройства: " + d.getType());
                builder.setPayload(
                        DeviceAddedEventAvro.newBuilder()
                                .setId(d.getId())
                                .setType(DeviceTypeAvro.valueOf(d.getType().name()))
                                .build()
                );
            }
            case DEVICE_REMOVED -> {
                System.out.println("Получено событие удаления устройства");
                DeviceRemovedEventProto d = event.getDeviceRemoved();
                System.out.println("Идентификатор устройства: " + d.getId());
                builder.setPayload(
                        DeviceRemovedEventAvro.newBuilder()
                                .setId(d.getId())
                                .build()
                );
            }
            case SCENARIO_ADDED -> {
                System.out.println("Получено событие добавления сценария");
                ScenarioAddedEventProto s = event.getScenarioAdded();
                System.out.println("Название сценария: " + s.getName());
                System.out.println("Количество условий: " + s.getConditionCount());
                for (ScenarioConditionProto c : s.getConditionList()) {
                    System.out.println("  Условие: sensor_id=" + c.getSensorId() +
                            ", тип=" + c.getType() + ", операция=" + c.getOperation());
                    if (c.hasBoolValue()) {
                        System.out.println("    значение (bool): " + c.getBoolValue());
                    } else if (c.hasIntValue()) {
                        System.out.println("    значение (int): " + c.getIntValue());
                    }
                }
                System.out.println("Количество действий: " + s.getActionCount());
                for (DeviceActionProto a : s.getActionList()) {
                    System.out.println("  Действие: sensor_id=" + a.getSensorId() +
                            ", тип=" + a.getType() +
                            (a.hasValue() ? ", значение=" + a.getValue() : ""));
                }
                builder.setPayload(
                        ScenarioAddedEventAvro.newBuilder()
                                .setName(s.getName())
                                .setConditions(s.getConditionList().stream()
                                        .map(c -> ScenarioConditionAvro.newBuilder()
                                                .setSensorId(c.getSensorId())
                                                .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                                                .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()))
                                                .setValue(extractConditionValue(c))
                                                .build())
                                        .toList())
                                .setActions(s.getActionList().stream()
                                        .map(a -> DeviceActionAvro.newBuilder()
                                                .setSensorId(a.getSensorId())
                                                .setType(ActionTypeAvro.valueOf(a.getType().name()))
                                                .setValue(a.hasValue() ? a.getValue() : null)
                                                .build())
                                        .toList())
                                .build()
                );
            }
            case SCENARIO_REMOVED -> {
                System.out.println("Получено событие удаления сценария");
                ScenarioRemovedEventProto s = event.getScenarioRemoved();
                System.out.println("Название сценария: " + s.getName());
                builder.setPayload(
                        ScenarioRemovedEventAvro.newBuilder()
                                .setName(s.getName())
                                .build()
                );
            }
            default -> throw new IllegalArgumentException("Unknown hub payload: " + event.getPayloadCase());
        }
        return builder.build();
    }

    private Object extractConditionValue(ScenarioConditionProto c) {
        if (c.hasBoolValue()) return c.getBoolValue();
        if (c.hasIntValue()) return c.getIntValue();
        return null;
    }

    public Instant protoTimestampToInstant(Timestamp ts) {
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }
}