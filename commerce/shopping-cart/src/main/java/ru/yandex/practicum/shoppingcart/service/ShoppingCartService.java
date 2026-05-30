package ru.yandex.practicum.shoppingcart.service;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.interaction.client.WarehouseClient;
import ru.yandex.practicum.interaction.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.shoppingcart.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.shoppingcart.mapper.ShoppingCartMapper;
import ru.yandex.practicum.shoppingcart.model.ShoppingCart;
import ru.yandex.practicum.shoppingcart.repository.ShoppingCartRepository;
import ru.yandex.practicum.interaction.exception.ProductInShoppingCartLowQuantityInWarehouse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoppingCartService {
    private final ShoppingCartRepository cartRepository;
    private final WarehouseClient warehouseClient;
    private final ShoppingCartMapper mapper;

    public ShoppingCartDto getCart(String username) {
        ShoppingCart cart = cartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> createEmptyCart(username));
        return mapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProducts(String username, Map<UUID, Long> products) {
        ShoppingCart cart = cartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> createEmptyCart(username));

        // Проверяем наличие на складе (через маппер)
        ShoppingCartDto cartDto = mapper.toDtoWithAddedProducts(cart, products);
        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(cartDto);
        } catch (FeignException.BadRequest e) {
            throw new ProductInShoppingCartLowQuantityInWarehouse(
                    "Not enough products in warehouse for cart " + cart.getShoppingCartId());
        } catch (FeignException | CallNotPermittedException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Warehouse is currently unavailable");
        }

        // Если проверка прошла – добавляем товары в корзину
        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            cart.getProducts().merge(entry.getKey(), entry.getValue(), Long::sum);
        }
        cart = cartRepository.save(cart);
        return mapper.toDto(cart);
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
        return mapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto changeQuantity(String username, ChangeProductQuantityRequest request) {
        ShoppingCart cart = cartRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NoProductsInShoppingCartException("Cart not found"));
        Map<UUID, Long> products = cart.getProducts();
        if (products == null) {
            products = new HashMap<>();
            cart.setProducts(products);
        }
        if (!products.containsKey(request.getProductId())) {
            throw new NoProductsInShoppingCartException("Product not in cart");
        }
        cart.getProducts().put(request.getProductId(), request.getNewQuantity());
        cart = cartRepository.save(cart);
        return mapper.toDto(cart);
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