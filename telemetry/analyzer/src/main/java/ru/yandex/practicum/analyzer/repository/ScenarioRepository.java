package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.analyzer.model.Scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    // Находит все сценарии для заданного хаба
    List<Scenario> findByHubId(String hubId);

    // Ищет сценарий по хабу и имени (уникальная пара)
    Optional<Scenario> findByHubIdAndName(String hubId, String name);

    @Query("SELECT DISTINCT s FROM Scenario s " +
            "LEFT JOIN FETCH s.scenarioConditions " +
            "LEFT JOIN FETCH s.scenarioActions " +
            "WHERE s.hubId = :hubId")
    List<Scenario> findByHubIdWithDetails(@Param("hubId") String hubId);
}