package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.mapper.ProtobufToAvroMapper;
import ru.yandex.practicum.collector.service.KafkaEventSender;
import ru.yandex.practicum.grpc.telemetry.event.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class MotionSensorEventHandler implements SensorEventHandler {

    private final KafkaEventSender kafkaEventSender;
    private final ProtobufToAvroMapper mapper;

    public MotionSensorEventHandler(KafkaEventSender kafkaEventSender, ProtobufToAvroMapper mapper) {
        this.kafkaEventSender = kafkaEventSender;
        this.mapper = mapper;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.MOTION_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        MotionSensorProto motion = event.getMotionSensor();
        System.out.println("Получено событие датчика движения");
        System.out.println("Качество связи: " + motion.getLinkQuality());
        System.out.println("Наличие движения: " + motion.getMotion());
        System.out.println("Напряжение: " + motion.getVoltage());

        SensorEventAvro avroEvent = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(mapper.protoTimestampToInstant(event.getTimestamp()))
                .setPayload(
                        MotionSensorAvro.newBuilder()
                                .setLinkQuality(motion.getLinkQuality())
                                .setMotion(motion.getMotion())
                                .setVoltage(motion.getVoltage())
                                .build()
                )
                .build();

        kafkaEventSender.sendSensorEvent(avroEvent);
    }
}