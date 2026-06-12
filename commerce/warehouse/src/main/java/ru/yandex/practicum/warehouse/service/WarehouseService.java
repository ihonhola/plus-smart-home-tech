package ru.yandex.practicum.warehouse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.dto.AddProductToWarehouseRequest;
import ru.yandex.practicum.interaction.dto.AddressDto;
import ru.yandex.practicum.interaction.dto.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.interaction.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.interaction.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.interaction.enums.QuantityState;
import ru.yandex.practicum.warehouse.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.interaction.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.warehouse.exception.OrderBookingNotFoundException;
import ru.yandex.practicum.warehouse.exception.ProductInShoppingCartNotInWarehouse;
import ru.yandex.practicum.warehouse.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.warehouse.model.OrderBooking;
import ru.yandex.practicum.warehouse.model.WarehouseProduct;
import ru.yandex.practicum.warehouse.model.WarehouseProduct.Dimension;
import ru.yandex.practicum.warehouse.repository.OrderBookingRepository;
import ru.yandex.practicum.warehouse.repository.WarehouseProductRepository;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseProductRepository warehouseRepository;
    private final OrderBookingRepository orderBookingRepository;

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

    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        // Проверяем наличие и уменьшаем остатки
        Map<UUID, Long> products = request.getProducts();
        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            long requiredQty = entry.getValue();
            WarehouseProduct wp = warehouseRepository.findByProductId(productId)
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException("Product not found in warehouse: " + productId));
            if (wp.getQuantity() < requiredQty) {
                throw new ProductInShoppingCartLowQuantityInWarehouse("Not enough quantity for product " + productId);
            }
            wp.setQuantity(wp.getQuantity() - requiredQty);
            warehouseRepository.save(wp);
        }

        // Рассчитываем характеристики (вес, объём, хрупкость)
        BookedProductsDto booked = calculateBookedProducts(products);

        // Сохраняем запись о бронировании
        OrderBooking booking = OrderBooking.builder()
                .orderId(request.getOrderId())
                .products(products)
                .deliveryWeight(booked.getDeliveryWeight())
                .deliveryVolume(booked.getDeliveryVolume())
                .fragile(booked.isFragile())
                .build();
        orderBookingRepository.save(booking);

        return booked;
    }

    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        OrderBooking booking = orderBookingRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new OrderBookingNotFoundException("Booking not found for order: " + request.getOrderId()));
        booking.setDeliveryId(request.getDeliveryId());
        orderBookingRepository.save(booking);
    }

    public void acceptReturn(Map<UUID, Long> products) {
        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            long qty = entry.getValue();
            WarehouseProduct wp = warehouseRepository.findByProductId(productId).orElse(null);
            if (wp != null) {
                wp.setQuantity(wp.getQuantity() + qty);
                warehouseRepository.save(wp);
            } else {
                log.warn("Return for non-existent product ignored: {}", productId);
            }
        }
    }

    private BookedProductsDto calculateBookedProducts(Map<UUID, Long> products) {
        double totalWeight = 0;
        double totalVolume = 0;
        boolean fragile = false;
        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            WarehouseProduct wp = warehouseRepository.findByProductId(entry.getKey())
                    .orElseThrow(() -> new ProductInShoppingCartNotInWarehouse("Product not found in warehouse"));
            long qty = entry.getValue();
            totalWeight += wp.getWeight() * qty;
            double vol = wp.getDimension().getWidth() * wp.getDimension().getHeight() * wp.getDimension().getDepth();
            totalVolume += vol * qty;
            if (wp.isFragile()) fragile = true;
        }
        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(fragile)
                .build();
    }
}