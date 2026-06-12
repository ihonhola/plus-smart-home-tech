package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.client.OrderClient;
import ru.yandex.practicum.interaction.client.ShoppingStoreClient;
import ru.yandex.practicum.interaction.dto.OrderDto;
import ru.yandex.practicum.interaction.dto.PaymentDto;
import ru.yandex.practicum.interaction.dto.ProductDto;
import ru.yandex.practicum.interaction.enums.PaymentStatus;
import ru.yandex.practicum.payment.exception.PaymentNotFoundException;
import ru.yandex.practicum.payment.model.Payment;
import ru.yandex.practicum.payment.repository.PaymentRepository;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final ShoppingStoreClient storeClient;

    //Рассчитать стоимость товаров в заказе
    @Transactional
    public PaymentDto calculateProductCost(UUID orderId) {
        OrderDto order = orderClient.getOrder(orderId);
        double productPrice = calculateProductsPrice(order.getProducts());
        return PaymentDto.builder()
                .orderId(orderId)
                .productPrice(productPrice)
                .build();
    }

    //Рассчитать полную стоимость заказа (включая налог и доставку)
    @Transactional
    public PaymentDto calculateTotalCost(UUID orderId) {
        OrderDto order = orderClient.getOrder(orderId);
        double productPrice = calculateProductsPrice(order.getProducts());
        double deliveryPrice = order.getDeliveryPrice();
        double tax = productPrice * 0.10; // НДС 10%
        double totalPrice = productPrice + tax + deliveryPrice;
        // Округляем до двух знаков
        totalPrice = Math.round(totalPrice * 100.0) / 100.0;
        return PaymentDto.builder()
                .orderId(orderId)
                .productPrice(productPrice)
                .deliveryPrice(deliveryPrice)
                .totalPrice(totalPrice)
                .build();
    }

    //Сохранить платёж
    @Transactional
    public PaymentDto createPayment(UUID orderId) {
        OrderDto order = orderClient.getOrder(orderId);
        double productPrice = calculateProductsPrice(order.getProducts());
        double deliveryPrice = order.getDeliveryPrice();
        double tax = productPrice * 0.10;
        double totalPrice = productPrice + tax + deliveryPrice;
        totalPrice = Math.round(totalPrice * 100.0) / 100.0;

        Payment payment = Payment.builder()
                .orderId(orderId)
                .productPrice(productPrice)
                .deliveryPrice(deliveryPrice)
                .totalPrice(totalPrice)
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);
        return toDto(payment);
    }

    //Успешная оплата
    @Transactional
    public PaymentDto successPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
        // Уведомляем сервис заказов
        orderClient.payment(payment.getOrderId());
        return toDto(payment);
    }

    //Ошибка оплаты
    @Transactional
    public PaymentDto failedPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        // Уведомляем сервис заказов
        orderClient.paymentFailed(payment.getOrderId());
        return toDto(payment);
    }

    //Сумма товаров
    private double calculateProductsPrice(Map<UUID, Long> products) {
        double total = 0.0;
        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            long quantity = entry.getValue();
            ProductDto product = storeClient.getProduct(productId);
            total += product.getPrice() * quantity;
        }
        return total;
    }

    private PaymentDto toDto(Payment payment) {
        return PaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .productPrice(payment.getProductPrice())
                .deliveryPrice(payment.getDeliveryPrice())
                .totalPrice(payment.getTotalPrice())
                .status(payment.getStatus())
                .build();
    }
}