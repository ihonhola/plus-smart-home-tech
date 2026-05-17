package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.mapper.ProtobufToAvroMapper;
import ru.yandex.practicum.collector.service.KafkaEventSender;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

@Component
public class TemperatureSensorEventHandler implements SensorEventHandler {

    private final KafkaEventSender kafkaEventSender;
    private final ProtobufToAvroMapper mapper;

    public TemperatureSensorEventHandler(KafkaEventSender kafkaEventSender, ProtobufToAvroMapper mapper) {
        this.kafkaEventSender = kafkaEventSender;
        this.mapper = mapper;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.TEMPERATURE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        TemperatureSensorProto temp = event.getTemperatureSensor();
        System.out.println("Получено событие датчика температуры");
        System.out.println("Температура в Цельсиях: " + temp.getTemperatureC());
        System.out.println("Температура в Фаренгейтах: " + temp.getTemperatureF());

        SensorEventAvro avroEvent = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(mapper.protoTimestampToInstant(event.getTimestamp()))
                .setPayload(
                        TemperatureSensorAvro.newBuilder()
                                .setTemperatureC(temp.getTemperatureC())
                                .setTemperatureF(temp.getTemperatureF())
                                .build()
                )
                .build();

        kafkaEventSender.sendSensorEvent(avroEvent);
    }
}