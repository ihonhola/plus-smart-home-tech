package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.analyzer.model.Sensor;

import java.util.Collection;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, String> {

    // Проверяет, существуют ли датчики с указанными id и принадлежащие конкретному хабу
    boolean existsByIdInAndHubId(Collection<String> ids, String hubId);

    // Ищет датчик по его id и hubId
    Optional<Sensor> findByIdAndHubId(String id, String hubId);
}