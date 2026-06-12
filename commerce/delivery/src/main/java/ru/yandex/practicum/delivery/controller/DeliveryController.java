package ru.yandex.practicum.delivery.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.interaction.dto.DeliveryDto;
import ru.yandex.practicum.interaction.dto.OrderDto;
import ru.yandex.practicum.delivery.service.DeliveryService;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PutMapping
    public ResponseEntity<DeliveryDto> planDelivery(@Valid @RequestBody DeliveryDto deliveryDto) {
        return ResponseEntity.ok(deliveryService.planDelivery(deliveryDto));
    }

    @PostMapping("/cost")
    public ResponseEntity<Double> deliveryCost(@Valid @RequestBody OrderDto order) {
        return ResponseEntity.ok(deliveryService.deliveryCost(order));
    }

    @PostMapping("/picked")
    public ResponseEntity<Void> deliveryPicked(@RequestBody UUID orderId) {
        deliveryService.deliveryPicked(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/successful")
    public ResponseEntity<Void> deliverySuccessful(@RequestBody UUID orderId) {
        deliveryService.deliverySuccessful(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/failed")
    public ResponseEntity<Void> deliveryFailed(@RequestBody UUID orderId) {
        deliveryService.deliveryFailed(orderId);
        return ResponseEntity.ok().build();
    }
}