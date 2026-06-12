package ru.yandex.practicum.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.interaction.enums.OrderState;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    private UUID orderId;

    private UUID shoppingCartId;

    private Map<UUID, Long> products;

    private UUID paymentId;

    private UUID deliveryId;

    private OrderState state;

    private double deliveryWeight;

    private double deliveryVolume;

    private boolean fragile;

    private double totalPrice;

    private double deliveryPrice;

    private double productPrice;

    private AddressDto toAddress;
}