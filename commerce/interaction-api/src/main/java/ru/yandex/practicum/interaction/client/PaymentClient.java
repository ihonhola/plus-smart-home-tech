package ru.yandex.practicum.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.interaction.dto.PaymentDto;
import java.util.UUID;

@FeignClient(name = "payment", path = "/api/v1/payment")
public interface PaymentClient {

    @PostMapping("/calculate/product")
    PaymentDto calculateProductCost(@RequestBody UUID orderId);

    @PostMapping("/calculate/total")
    PaymentDto calculateTotalCost(@RequestBody UUID orderId);

    @PostMapping
    PaymentDto createPayment(@RequestBody UUID orderId);
}