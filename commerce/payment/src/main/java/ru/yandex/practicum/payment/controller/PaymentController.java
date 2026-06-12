package ru.yandex.practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.interaction.dto.PaymentDto;
import ru.yandex.practicum.payment.service.PaymentService;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // POST /api/v1/payment/calculate/product – расчёт стоимости товаров
    @PostMapping("/calculate/product")
    public ResponseEntity<PaymentDto> calculateProductCost(@RequestBody UUID orderId) {
        return ResponseEntity.ok(paymentService.calculateProductCost(orderId));
    }

    // POST /api/v1/payment/calculate/total – расчёт общей стоимости
    @PostMapping("/calculate/total")
    public ResponseEntity<PaymentDto> calculateTotalCost(@RequestBody UUID orderId) {
        return ResponseEntity.ok(paymentService.calculateTotalCost(orderId));
    }

    // POST /api/v1/payment – создание платежа
    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(@RequestBody UUID orderId) {
        return ResponseEntity.ok(paymentService.createPayment(orderId));
    }

    // POST /api/v1/payment/{paymentId}/success
    @PostMapping("/{paymentId}/success")
    public ResponseEntity<PaymentDto> successPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.successPayment(paymentId));
    }

    // POST /api/v1/payment/{paymentId}/failed
    @PostMapping("/{paymentId}/failed")
    public ResponseEntity<PaymentDto> failedPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.failedPayment(paymentId));
    }
}