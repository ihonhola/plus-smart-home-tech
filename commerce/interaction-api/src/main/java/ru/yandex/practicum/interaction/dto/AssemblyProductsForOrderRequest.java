package ru.yandex.practicum.interaction.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssemblyProductsForOrderRequest {
    @NotEmpty
    private Map<UUID, Long> products;

    @NotNull
    private UUID orderId;
}