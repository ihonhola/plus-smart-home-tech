package ru.yandex.practicum.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.interaction.enums.PaymentStatus;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {
    private UUID paymentId;

    private UUID orderId;

    private double productPrice;

    private double deliveryPrice;

    private double totalPrice;

    private PaymentStatus status;
}