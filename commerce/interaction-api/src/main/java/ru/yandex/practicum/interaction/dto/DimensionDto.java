package ru.yandex.practicum.interaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Valid @NotNull
public class DimensionDto {
    @Min(1)
    private double width;

    @Min(1)
    private double height;

    @Min(1)
    private double depth;
}