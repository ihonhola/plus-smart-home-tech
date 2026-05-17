package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.mapper.ProtobufToAvroMapper;
import ru.yandex.practicum.collector.service.KafkaEventSender;
import ru.yandex.practicum.grpc.telemetry.event.ClimateSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class ClimateSensorEventHandler implements SensorEventHandler {

    private final KafkaEventSender kafkaEventSender;
    private final ProtobufToAvroMapper mapper;

    public ClimateSensorEventHandler(KafkaEventSender kafkaEventSender, ProtobufToAvroMapper mapper) {
        this.kafkaEventSender = kafkaEventSender;
        this.mapper = mapper;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.CLIMATE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        ClimateSensorProto climate = event.getClimateSensor();
        System.out.println("Получено событие климатического датчика");
        System.out.println("Температура: " + climate.getTemperatureC());
        System.out.println("Влажность: " + climate.getHumidity());
        System.out.println("Уровень CO2: " + climate.getCo2Level());

        SensorEventAvro avroEvent = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(mapper.protoTimestampToInstant(event.getTimestamp()))
                .setPayload(
                        ClimateSensorAvro.newBuilder()
                                .setTemperatureC(climate.getTemperatureC())
                                .setHumidity(climate.getHumidity())
                                .setCo2Level(climate.getCo2Level())
                                .build()
                )
                .build();

        kafkaEventSender.sendSensorEvent(avroEvent);
    }
}