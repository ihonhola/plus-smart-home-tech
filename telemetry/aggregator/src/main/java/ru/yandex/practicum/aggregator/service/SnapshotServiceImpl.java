package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SnapshotServiceImpl implements SnapshotService {

    private final Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();

    /**
     * Обновляет снапшот для хаба из события датчика.
     * @param event событие датчика
     * @return Optional с обновлённым снапшотом, если состояние изменилось, иначе empty
     */
    @Override
    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();
        Instant eventTime = event.getTimestamp();
        Object eventData = event.getPayload();

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId, k ->
                SensorsSnapshotAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(eventTime)
                        .setSensorsState(new java.util.HashMap<>())
                        .build()
        );

        // Проверяем, есть ли уже состояние этого датчика
        if (snapshot.getSensorsState().containsKey(sensorId)) {
            SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);
            // Если старые данные новее или идентичны — не обновляем
            if (oldState.getTimestamp().isAfter(eventTime) ||
                    oldState.getData().equals(eventData)) {
                return Optional.empty();
            }
        }

        // Создаём новое состояние
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(eventTime)
                .setData(eventData)
                .build();

        // Обновляем снапшот
        snapshot.setTimestamp(eventTime); // время снапшота = последнее событие
        snapshot.getSensorsState().put(sensorId, newState);

        log.info("Обновлён снапшот для хаба {}: датчик {} = {}", hubId, sensorId, eventData);
        return Optional.of(snapshot);
    }
}