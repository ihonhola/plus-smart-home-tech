package ru.yandex.practicum.warehouse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.client.ShoppingStoreClient;
import ru.yandex.practicum.interaction.dto.AddProductToWarehouseRequest;
import ru.yandex.practicum.interaction.dto.AddressDto;
import ru.yandex.practicum.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.interaction.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.interaction.enums.QuantityState;
import ru.yandex.practicum.warehouse.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.warehouse.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.warehouse.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.warehouse.model.WarehouseProduct;
import ru.yandex.practicum.warehouse.model.WarehouseProduct.Dimension;
import ru.yandex.practicum.warehouse.repository.WarehouseProductRepository;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseProductRepository warehouseRepository;
    private final ShoppingStoreClient storeClient;

    private static final String[] ADDRESSES = {"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS =
            ADDRESSES[new Random(new SecureRandom().nextLong()).nextInt(ADDRESSES.length)];

    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        // проверим, что такого товара ещё нет на складе
        warehouseRepository.findByProductId(request.getProductId()).ifPresent(p -> {
            throw new SpecifiedProductAlreadyInWarehouseException("Product already exists");
        });

        WarehouseProduct product = WarehouseProduct.builder()
                .productId(request.getProductId())
                .fragile(request.isFragile())
                .dimension(new Dimension(
                        request.getDimension().getWidth(),
                        request.getDimension().getHeight(),
                        request.getDimension().getDepth()))
                .weight(request.getWeight())
                .quantity(0)  // начальное количество ноль, затем добавим через addProduct
                .build();
        warehouseRepository.save(product);
    }

    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        WarehouseProduct product = warehouseRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException("Product not found in warehouse"));
        product.setQuantity(product.getQuantity() + request.getQuantity());
        warehouseRepository.save(product);
/*
        // обновляем состояние количества в магазине (через Feign)
        storeClient.setProductQuantityState(
                request.getProductId(),
                getQuantityState(product.getQuantity()).name()
        );*/
    }

    private QuantityState getQuantityState(long quantity) {
        if (quantity == 0) return QuantityState.ENDED;
        if (quantity < 10) return QuantityState.FEW;
        if (quantity <= 100) return QuantityState.ENOUGH;
        return QuantityState.MANY;
    }

    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        Map<UUID, Long> products = cart.getProducts();
        double totalWeight = 0;
        double totalVolume = 0;
        boolean fragile = false;

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            long requestedQuantity = entry.getValue();
            WarehouseProduct wp = warehouseRepository.findByProductId(productId)
                    .orElseThrow(() -> new ProductInShoppingCartLowQuantityInWarehouse(
                            "Product " + productId + " not found in warehouse"));
            if (wp.getQuantity() < requestedQuantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouse(
                        "Not enough quantity for product " + productId);
            }
            totalWeight += wp.getWeight() * requestedQuantity;
            double volume = wp.getDimension().getWidth() * wp.getDimension().getHeight() * wp.getDimension().getDepth();
            totalVolume += volume * requestedQuantity;
            if (wp.isFragile()) fragile = true;
        }

        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(fragile)
                .build();
    }

    public AddressDto getWarehouseAddress() {
        // преобразуем CURRENT_ADDRESS в AddressDto
        return AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();
    }
}