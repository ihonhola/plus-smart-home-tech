package ru.yandex.practicum.shoppingcart.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.client.ShoppingStoreClient;
import ru.yandex.practicum.interaction.client.WarehouseClient;
import ru.yandex.practicum.interaction.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.shoppingcart.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.shoppingcart.model.ShoppingCart;
import ru.yandex.practicum.shoppingcart.repository.ShoppingCartRepository;
import ru.yandex.practicum.warehouse.exception.ProductInShoppingCartLowQuantityInWarehouse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoppingCartService {
    private final ShoppingCartRepository cartRepository;
    private final ShoppingStoreClient storeClient;
    private final WarehouseClient warehouseClient;

    private ShoppingCartDto toDto(ShoppingCart cart) {
        return ShoppingCartDto.builder()
                .shoppingCartId(cart.getShoppingCartId())
                .products(cart.getProducts())
                .build();
    }

    public ShoppingCartDto getCart(String username) {
        ShoppingCart cart = cartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> createEmptyCart(username));
        return toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProducts(String username, Map<UUID, Long> products) {
        ShoppingCart cart = cartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> createEmptyCart(username));

        // объединяем то, что будет в корзине после добавления
        Map<UUID, Long> combined = new HashMap<>(cart.getProducts());
        products.forEach((id, qty) -> combined.merge(id, qty, Long::sum));

        // проверяем наличие на складе
        ShoppingCartDto cartDto = ShoppingCartDto.builder()
                .shoppingCartId(cart.getShoppingCartId())
                .products(combined)
                .build();

        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(cartDto);
        } catch (FeignException.BadRequest e) {
            throw new ProductInShoppingCartLowQuantityInWarehouse(
                    "Not enough products in warehouse for cart " + cart.getShoppingCartId());
        }

        // если проверка прошла – добавляем товары
        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            cart.getProducts().merge(entry.getKey(), entry.getValue(), Long::sum);
        }

        cart = cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public ShoppingCartDto removeProducts(String username, List<UUID> productIds) {
        ShoppingCart cart = cartRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NoProductsInShoppingCartException("Cart not found"));
        Map<UUID, Long> current = cart.getProducts();
        if (productIds.stream().noneMatch(current::containsKey)) {
            throw new NoProductsInShoppingCartException("No such products in cart");
        }
        productIds.forEach(current::remove);
        cart = cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public ShoppingCartDto changeQuantity(String username, ChangeProductQuantityRequest request) {
        ShoppingCart cart = cartRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NoProductsInShoppingCartException("Cart not found"));
        if (!cart.getProducts().containsKey(request.getProductId())) {
            throw new NoProductsInShoppingCartException("Product not in cart");
        }
        cart.getProducts().put(request.getProductId(), request.getNewQuantity());
        cart = cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public void deactivate(String username) {
        ShoppingCart cart = cartRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NoProductsInShoppingCartException("Cart not found"));
        cart.setActive(false);
        cartRepository.save(cart);
    }

    private ShoppingCart createEmptyCart(String username) {
        ShoppingCart cart = ShoppingCart.builder()
                .username(username)
                .active(true)
                .products(new HashMap<>())
                .build();
        return cartRepository.save(cart);
    }
}