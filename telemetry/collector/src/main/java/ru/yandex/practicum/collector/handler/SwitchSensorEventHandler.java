package ru.yandex.practicum.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.mapper.ProtobufToAvroMapper;
import ru.yandex.practicum.collector.service.KafkaEventSender;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SwitchSensorProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;

@Component
public class SwitchSensorEventHandler implements SensorEventHandler {

    private final KafkaEventSender kafkaEventSender;
    private final ProtobufToAvroMapper mapper;

    public SwitchSensorEventHandler(KafkaEventSender kafkaEventSender, ProtobufToAvroMapper mapper) {
        this.kafkaEventSender = kafkaEventSender;
        this.mapper = mapper;
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        SwitchSensorProto sw = event.getSwitchSensor();
        System.out.println("Получено событие датчика переключателя");
        System.out.println("Состояние: " + (sw.getState() ? "включён" : "выключен"));

        SensorEventAvro avroEvent = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(mapper.protoTimestampToInstant(event.getTimestamp()))
                .setPayload(
                        SwitchSensorAvro.newBuilder()
                                .setState(sw.getState())
                                .build()
                )
                .build();

        kafkaEventSender.sendSensorEvent(avroEvent);
    }
}