package ru.yandex.practicum.collector.model.sensor;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@JsonTypeName("TEMPERATURE_SENSOR_EVENT")
public class TemperatureSensorEvent extends SensorEvent {

    @NotNull
    private Integer temperatureC;

    @NotNull
    private Integer temperatureF;

    @Override
    public SensorEventType getType() {
        return SensorEventType.TEMPERATURE_SENSOR_EVENT;
    }
}