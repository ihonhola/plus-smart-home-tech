package ru.yandex.practicum.hubrouter;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.*;

import java.time.Instant;
import java.util.Random;

@Service
public class EventDataProducer {

    private static final Logger log = LoggerFactory.getLogger(EventDataProducer.class);
    private final Random random = new Random();

    @GrpcClient("collector")
    private CollectorControllerGrpc.CollectorControllerBlockingStub collectorStub;

    // Инжектим идентификаторы датчиков из application.yaml (можно через @ConfigurationProperties)
    // Для простоты пока один датчик движения и температуры.

    @Scheduled(initialDelay = 1000, fixedDelay = 5000)
    public void sendMotionSensorEvent() {
        try {
            SensorEventProto event = SensorEventProto.newBuilder()
                    .setId("motion-1")
                    .setHubId("hub-1")  // в реальности hubId будет определяться, пока так
                    .setTimestamp(toTimestamp(Instant.now()))
                    .setMotionSensor(
                            MotionSensorProto.newBuilder()
                                    .setLinkQuality(random.nextInt(101))  // 0..100
                                    .setMotion(random.nextBoolean())
                                    .setVoltage(random.nextInt(221))      // 0..220
                                    .build()
                    )
                    .build();
            log.info("Sending motion event: {}", event.getAllFields());
            // Вызов gRPC-метода
            collectorStub.collectSensorEvent(event);
            log.info("Motion event sent successfully");
        } catch (StatusRuntimeException e) {
            log.error("gRPC error: {}", e.getStatus());
        }
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}