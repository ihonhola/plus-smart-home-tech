package ru.yandex.practicum.delivery.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.client.OrderClient;
import ru.yandex.practicum.interaction.client.WarehouseClient;
import ru.yandex.practicum.interaction.dto.AddressDto;
import ru.yandex.practicum.interaction.dto.DeliveryDto;
import ru.yandex.practicum.interaction.dto.OrderDto;
import ru.yandex.practicum.interaction.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.interaction.enums.DeliveryState;
import ru.yandex.practicum.delivery.exception.NoDeliveryFoundException;
import ru.yandex.practicum.delivery.model.AddressEmbeddable;
import ru.yandex.practicum.delivery.model.Delivery;
import ru.yandex.practicum.delivery.repository.DeliveryRepository;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final WarehouseClient warehouseClient;
    private final OrderClient orderClient;

    //Создать доставку
    @Transactional
    public DeliveryDto planDelivery(DeliveryDto request) {
        // Получаем адрес склада (fromAddress), берём из warehouse
        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();
        AddressEmbeddable from = mapToEmbeddable(warehouseAddress);
        AddressEmbeddable to = mapToEmbeddable(request.getToAddress());

        Delivery delivery = Delivery.builder()
                .orderId(request.getOrderId())
                .fromAddress(from)
                .toAddress(to)
                .deliveryState(DeliveryState.CREATED)
                .build();
        delivery = deliveryRepository.save(delivery);
        return toDto(delivery);
    }

    //Расчёт стоимости доставки
    public Double deliveryCost(OrderDto order) {
        // Получаем адрес склада
        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();
        double base = 5.0;
        double multiplier = warehouseAddress.getCountry().equals("ADDRESS_2") ? 2.0 : 1.0;
        double cost = base * (1 + multiplier);

        // Признак хрупкости
        if (order.isFragile()) {
            cost *= 1.2;
        }

        // Вес и объём
        cost += order.getDeliveryWeight() * 0.3;
        cost += order.getDeliveryVolume() * 0.2;

        // Сравнение улиц
        if (!order.getToAddress().getStreet().equals(warehouseAddress.getStreet())) {
            cost *= 1.2;
        }

        // Округление до двух знаков
        cost = Math.round(cost * 100.0) / 100.0;
        return cost;
    }

    //Принять товары в доставку (picked)
    @Transactional
    public void deliveryPicked(UUID orderId) {
        // 1. Найти доставку по orderId
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Delivery not found for order: " + orderId));

        // 2. Изменить статус доставки на IN_PROGRESS
        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);

        // 3. Сообщить сервису заказов, что сборка завершена (ASSEMBLED)
        orderClient.assembly(orderId);

        // 4. Уведомить склад о передаче в доставку (связать deliveryId)
        ShippedToDeliveryRequest shippedRequest = new ShippedToDeliveryRequest();
        shippedRequest.setOrderId(orderId);
        shippedRequest.setDeliveryId(delivery.getDeliveryId());
        warehouseClient.shippedToDelivery(shippedRequest);
    }

    //Успешная доставка
    @Transactional
    public void deliverySuccessful(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Delivery not found for order: " + orderId));
        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);
        orderClient.delivery(orderId);
    }

    //Ошибка доставки
    @Transactional
    public void deliveryFailed(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Delivery not found for order: " + orderId));
        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);
        orderClient.deliveryFailed(orderId);
    }

    private AddressEmbeddable mapToEmbeddable(AddressDto dto) {
        return AddressEmbeddable.builder()
                .country(dto.getCountry())
                .city(dto.getCity())
                .street(dto.getStreet())
                .house(dto.getHouse())
                .flat(dto.getFlat())
                .build();
    }

    private DeliveryDto toDto(Delivery delivery) {
        return DeliveryDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .fromAddress(mapToDto(delivery.getFromAddress()))
                .toAddress(mapToDto(delivery.getToAddress()))
                .orderId(delivery.getOrderId())
                .deliveryState(delivery.getDeliveryState())
                .build();
    }

    private AddressDto mapToDto(AddressEmbeddable emb) {
        return AddressDto.builder()
                .country(emb.getCountry())
                .city(emb.getCity())
                .street(emb.getStreet())
                .house(emb.getHouse())
                .flat(emb.getFlat())
                .build();
    }
}