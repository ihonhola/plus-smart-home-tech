package ru.yandex.practicum.shoppingcart.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.shoppingcart.model.ShoppingCart;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ShoppingCartMapper {

    public ShoppingCartDto toDto(ShoppingCart cart) {
        Map<UUID, Long> products = cart.getProducts();
        if (products == null) {
            products = new HashMap<>();
        }
        return ShoppingCartDto.builder()
                .shoppingCartId(cart.getShoppingCartId())
                .products(new HashMap<>(products)) // защитная копия
                .build();
    }

    public ShoppingCartDto toDtoWithAddedProducts(ShoppingCart cart, Map<UUID, Long> newProducts) {
        Map<UUID, Long> combined = new HashMap<>(cart.getProducts() != null ? cart.getProducts() : Map.of());
        newProducts.forEach((id, qty) -> combined.merge(id, qty, Long::sum));
        return ShoppingCartDto.builder()
                .shoppingCartId(cart.getShoppingCartId())
                .products(combined)
                .build();
    }
}