package ru.yandex.practicum.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.interaction.dto.CreateNewOrderRequest;
import ru.yandex.practicum.interaction.dto.OrderDto;
import ru.yandex.practicum.interaction.dto.ProductReturnRequest;
import ru.yandex.practicum.order.service.OrderService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDto>> getClientOrders(@RequestParam String username) {
        return ResponseEntity.ok(orderService.getClientOrders(username));
    }

    @PutMapping
    public ResponseEntity<OrderDto> createNewOrder(@Valid @RequestBody CreateNewOrderRequest request,
                                                   @RequestParam String username) {
        return ResponseEntity.ok(orderService.createNewOrder(request, username));
    }

    @PostMapping("/payment")
    public ResponseEntity<OrderDto> payment(@RequestBody UUID orderId) {
        return ResponseEntity.ok(orderService.payment(orderId));
    }

    @PostMapping("/payment/failed")
    public ResponseEntity<OrderDto> paymentFailed(@RequestBody UUID orderId) {
        return ResponseEntity.ok(orderService.paymentFailed(orderId));
    }

    @PostMapping("/delivery")
    public ResponseEntity<OrderDto> delivery(@RequestBody UUID orderId) {
        return ResponseEntity.ok(orderService.delivery(orderId));
    }

    @PostMapping("/delivery/failed")
    public ResponseEntity<OrderDto> deliveryFailed(@RequestBody UUID orderId) {
        return ResponseEntity.ok(orderService.deliveryFailed(orderId));
    }

    @PostMapping("/completed")
    public ResponseEntity<OrderDto> complete(@RequestBody UUID orderId) {
        return ResponseEntity.ok(orderService.complete(orderId));
    }

    @PostMapping("/assembly")
    public ResponseEntity<OrderDto> assembly(@RequestBody UUID orderId) {
        return ResponseEntity.ok(orderService.assembly(orderId));
    }

    @PostMapping("/assembly/failed")
    public ResponseEntity<OrderDto> assemblyFailed(@RequestBody UUID orderId) {
        return ResponseEntity.ok(orderService.assemblyFailed(orderId));
    }

    @PostMapping("/return")
    public ResponseEntity<OrderDto> productReturn(@Valid @RequestBody ProductReturnRequest request) {
        return ResponseEntity.ok(orderService.productReturn(request));
    }
}