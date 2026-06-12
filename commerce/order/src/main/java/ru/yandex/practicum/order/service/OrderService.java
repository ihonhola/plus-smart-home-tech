package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.client.DeliveryClient;
import ru.yandex.practicum.interaction.client.PaymentClient;
import ru.yandex.practicum.interaction.client.ShoppingStoreClient;
import ru.yandex.practicum.interaction.client.WarehouseClient;
import ru.yandex.practicum.interaction.dto.AddressDto;
import ru.yandex.practicum.interaction.dto.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.interaction.dto.CreateNewOrderRequest;
import ru.yandex.practicum.interaction.dto.DeliveryDto;
import ru.yandex.practicum.interaction.dto.OrderDto;
import ru.yandex.practicum.interaction.dto.ProductReturnRequest;
import ru.yandex.practicum.interaction.dto.ShoppingCartDto;
import ru.yandex.practicum.interaction.enums.DeliveryState;
import ru.yandex.practicum.interaction.enums.OrderState;
import ru.yandex.practicum.order.exception.NoOrderFoundException;
import ru.yandex.practicum.order.model.AddressEmbeddable;
import ru.yandex.practicum.order.model.Order;
import ru.yandex.practicum.order.repository.OrderRepository;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final DeliveryClient deliveryClient;
    private final PaymentClient paymentClient;
    private final WarehouseClient warehouseClient;

    public List<OrderDto> getClientOrders(String username) {
        return orderRepository.findByUsername(username).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public OrderDto createNewOrder(CreateNewOrderRequest request, String username) {
        ShoppingCartDto cart = request.getShoppingCart();

        // 1. Создаём заказ со статусом NEW (без сборки)
        Order order = Order.builder()
                .username(username)
                .shoppingCartId(cart.getShoppingCartId())
                .products(cart.getProducts())
                .toAddress(mapToEmbeddable(request.getDeliveryAddress()))
                .state(OrderState.NEW)
                .build();
        order = orderRepository.save(order);
        UUID orderId = order.getOrderId();

        // 2. Сборка товаров на складе (уменьшает остатки, возвращает BookedProductsDto)
        AssemblyProductsForOrderRequest assemblyReq = new AssemblyProductsForOrderRequest();
        assemblyReq.setProducts(cart.getProducts());
        assemblyReq.setOrderId(orderId);
        BookedProductsDto booked = warehouseClient.assemblyProductsForOrder(assemblyReq);
        order.setDeliveryWeight(booked.getDeliveryWeight());
        order.setDeliveryVolume(booked.getDeliveryVolume());
        order.setFragile(booked.isFragile());

        // 3. Создаём доставку (planDelivery) – адрес склада получаем через Feign
        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();
        DeliveryDto deliveryRequest = DeliveryDto.builder()
                .fromAddress(warehouseAddress)
                .toAddress(request.getDeliveryAddress())
                .orderId(orderId)
                .deliveryState(DeliveryState.CREATED)
                .build();
        DeliveryDto delivery = deliveryClient.planDelivery(deliveryRequest);
        order.setDeliveryId(delivery.getDeliveryId());

        // 4. Расчёт стоимости доставки
        double deliveryCost = deliveryClient.deliveryCost(toDto(order));

        // 5. Расчёт стоимости товаров (через payment-сервис)
        double productCost = paymentClient.calculateProductCost(orderId).getProductPrice();

        // 6. Общая стоимость (через payment-сервис)
        double totalCost = paymentClient.calculateTotalCost(orderId).getTotalPrice();

        order.setDeliveryPrice(deliveryCost);
        order.setProductPrice(productCost);
        order.setTotalPrice(totalCost);

        order = orderRepository.save(order);
        return toDto(order);
    }

    public OrderDto getOrder(UUID orderId) {
        return toDto(findOrder(orderId));
    }

    @Transactional
    public OrderDto payment(UUID orderId) {
        Order order = findOrder(orderId);
        order.setState(OrderState.PAID);
        return toDto(order);
    }

    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        Order order = findOrder(orderId);
        order.setState(OrderState.PAYMENT_FAILED);
        return toDto(order);
    }

    @Transactional
    public OrderDto delivery(UUID orderId) {
        Order order = findOrder(orderId);
        order.setState(OrderState.DELIVERED);
        return toDto(order);
    }

    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        Order order = findOrder(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        return toDto(order);
    }

    @Transactional
    public OrderDto complete(UUID orderId) {
        Order order = findOrder(orderId);
        order.setState(OrderState.COMPLETED);
        return toDto(order);
    }

    @Transactional
    public OrderDto assembly(UUID orderId) {
        Order order = findOrder(orderId);
        order.setState(OrderState.ASSEMBLED);
        return toDto(order);
    }

    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        Order order = findOrder(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        return toDto(order);
    }

    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        Order order = findOrder(request.getOrderId());
        // логика возврата: уменьшаем количество или меняем статус
        order.setState(OrderState.PRODUCT_RETURNED);
        return toDto(order);
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));
    }

    private OrderDto toDto(Order order) {
        return OrderDto.builder()
                .orderId(order.getOrderId())
                .shoppingCartId(order.getShoppingCartId())
                .products(order.getProducts())
                .paymentId(order.getPaymentId())
                .deliveryId(order.getDeliveryId())
                .state(order.getState())
                .deliveryWeight(order.getDeliveryWeight())
                .deliveryVolume(order.getDeliveryVolume())
                .fragile(order.isFragile())
                .totalPrice(order.getTotalPrice())
                .deliveryPrice(order.getDeliveryPrice())
                .productPrice(order.getProductPrice())
                .toAddress(mapToDto(order.getToAddress()))
                .build();
    }

    private AddressDto mapToDto(AddressEmbeddable emb) {
        if (emb == null) return null;
        return AddressDto.builder()
                .country(emb.getCountry())
                .city(emb.getCity())
                .street(emb.getStreet())
                .house(emb.getHouse())
                .flat(emb.getFlat())
                .build();
    }

    private AddressEmbeddable mapToEmbeddable(AddressDto dto) {
        if (dto == null) return null;
        return AddressEmbeddable.builder()
                .country(dto.getCountry())
                .city(dto.getCity())
                .street(dto.getStreet())
                .house(dto.getHouse())
                .flat(dto.getFlat())
                .build();
    }
}