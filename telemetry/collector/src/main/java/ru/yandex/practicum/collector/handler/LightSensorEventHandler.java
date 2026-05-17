package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.mapper.ProtobufToAvroMapper;
import ru.yandex.practicum.collector.service.KafkaEventSender;
import ru.yandex.practicum.grpc.telemetry.event.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class LightSensorEventHandler implements SensorEventHandler {

    private final KafkaEventSender kafkaEventSender;
    private final ProtobufToAvroMapper mapper;

    public LightSensorEventHandler(KafkaEventSender kafkaEventSender, ProtobufToAvroMapper mapper) {
        this.kafkaEventSender = kafkaEventSender;
        this.mapper = mapper;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.LIGHT_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        LightSensorProto light = event.getLightSensor();
        System.out.println("Получено событие датчика освещённости");
        System.out.println("Уровень освещённости: " + light.getLuminosity());
        System.out.println("Качество связи: " + light.getLinkQuality());

        SensorEventAvro avroEvent = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(mapper.protoTimestampToInstant(event.getTimestamp()))
                .setPayload(
                        LightSensorAvro.newBuilder()
                                .setLinkQuality(light.getLinkQuality())
                                .setLuminosity(light.getLuminosity())
                                .build()
                )
                .build();

        kafkaEventSender.sendSensorEvent(avroEvent);
    }
}