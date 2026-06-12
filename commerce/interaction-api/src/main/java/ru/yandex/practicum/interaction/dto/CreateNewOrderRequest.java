package ru.yandex.practicum.interaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewOrderRequest {
    @NotNull
    @Valid
    private ShoppingCartDto shoppingCart;

    @NotNull
    @Valid
    private AddressDto deliveryAddress;
}