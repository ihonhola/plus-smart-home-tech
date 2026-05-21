package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.Condition;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.analyzer.model.ScenarioActionId;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.analyzer.model.ScenarioConditionId;
import ru.yandex.practicum.analyzer.model.Sensor;
import ru.yandex.practicum.analyzer.repository.ActionRepository;
import ru.yandex.practicum.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {
    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Transactional
    public void processHubEvent(HubEventAvro event) {
        String hubId = event.getHubId();
        Object payload = event.getPayload();
        if (payload instanceof DeviceAddedEventAvro added) {
            handleDeviceAdded(hubId, added);
        } else if (payload instanceof DeviceRemovedEventAvro removed) {
            handleDeviceRemoved(hubId, removed);
        } else if (payload instanceof ScenarioAddedEventAvro addedScenario) {
            handleScenarioAdded(hubId, addedScenario);
        } else if (payload instanceof ScenarioRemovedEventAvro removedScenario) {
            handleScenarioRemoved(hubId, removedScenario);
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        Sensor sensor = Sensor.builder()
                .id(event.getId())
                .hubId(hubId)
                .build();
        sensorRepository.save(sensor);
        log.info("Добавлен датчик {} в хаб {}", sensor.getId(), hubId);
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        String sensorId = event.getId();
        sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensor -> {
            sensorRepository.delete(sensor);
            log.info("Удалён датчик {} из хаба {}", sensorId, hubId);
        });
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        // Проверяем существование сценария с таким именем
        scenarioRepository.findByHubIdAndName(hubId, event.getName()).ifPresent(s -> {
            throw new IllegalArgumentException("Сценарий " + event.getName() + " уже существует");
        });

        Scenario scenario = Scenario.builder()
                .hubId(hubId)
                .name(event.getName())
                .build();

        // Сохраняем условия
        var conditions = event.getConditions().stream().map(condAvro -> {
            Condition c = Condition.builder()
                    .type(condAvro.getType().name())
                    .operation(condAvro.getOperation().name())
                    .value(extractConditionValue(condAvro))
                    .build();
            return conditionRepository.save(c);
        }).collect(Collectors.toList());

        // Сохраняем действия
        var actions = event.getActions().stream().map(actAvro -> {
            Action a = Action.builder()
                    .type(actAvro.getType().name())
                    .value(actAvro.getValue())
                    .build();
            return actionRepository.save(a);
        }).collect(Collectors.toList());

        // Создаём связи сценария с условиями и датчиками
        // Внимание: здесь предполагается, что порядок условий/действий соответствует порядку в Avro
        // Датчик берётся из sensor_id
        for (int i = 0; i < event.getConditions().size(); i++) {
            ScenarioConditionAvro c = event.getConditions().get(i);
            Sensor sensor = sensorRepository.findById(c.getSensorId()).orElseThrow(() ->
                    new IllegalStateException("Датчик " + c.getSensorId() + " не найден"));
            ScenarioCondition sc = ScenarioCondition.builder()
                    .id(new ScenarioConditionId(scenario.getId(), sensor.getId(), conditions.get(i).getId()))
                    .scenario(scenario)
                    .sensor(sensor)
                    .condition(conditions.get(i))
                    .build();
            scenario.getScenarioConditions().add(sc);
        }

        for (int i = 0; i < event.getActions().size(); i++) {
            DeviceActionAvro a = event.getActions().get(i);
            Sensor sensor = sensorRepository.findById(a.getSensorId()).orElseThrow(() ->
                    new IllegalStateException("Датчик " + a.getSensorId() + " не найден"));
            ScenarioAction sa = ScenarioAction.builder()
                    .id(new ScenarioActionId(scenario.getId(), sensor.getId(), actions.get(i).getId()))
                    .scenario(scenario)
                    .sensor(sensor)
                    .action(actions.get(i))
                    .build();
            scenario.getScenarioActions().add(sa);
        }

        scenarioRepository.save(scenario);
        log.info("Добавлен сценарий '{}' в хаб {}", event.getName(), hubId);
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        scenarioRepository.findByHubIdAndName(hubId, event.getName())
                .ifPresent(scenario -> {
                    scenarioRepository.delete(scenario);
                    log.info("Удалён сценарий '{}' из хаба {}", event.getName(), hubId);
                });
    }

    private Integer extractConditionValue(ScenarioConditionAvro cond) {
        if (cond.getValue() instanceof Boolean b) return b ? 1 : 0;
        if (cond.getValue() instanceof Integer i) return i;
        return null;
    }
}