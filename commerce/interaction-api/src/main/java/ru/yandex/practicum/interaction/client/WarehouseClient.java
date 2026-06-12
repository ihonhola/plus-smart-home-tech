package ru.yandex.practicum.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.interaction.dto.AddProductToWarehouseRequest;
import ru.yandex.practicum.interaction.dto.AddressDto;
import ru.yandex.practicum.interaction.dto.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.interaction.dto.BookedProductsDto;
import ru.yandex.practicum.interaction.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.interaction.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.interaction.dto.ShoppingCartDto;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "warehouse", path = "/api/v1/warehouse")
public interface WarehouseClient {

    @PutMapping
    void newProductInWarehouse(@RequestBody NewProductInWarehouseRequest request);

    @PostMapping("/add")
    void addProductToWarehouse(@RequestBody AddProductToWarehouseRequest request);

    @PostMapping("/check")
    BookedProductsDto checkProductQuantityEnoughForShoppingCart(@RequestBody ShoppingCartDto cartDto);

    @GetMapping("/address")
    AddressDto getWarehouseAddress();

    @PostMapping("/assembly")
    BookedProductsDto assemblyProductsForOrder(@RequestBody AssemblyProductsForOrderRequest request);

    @PostMapping("/shipped")
    void shippedToDelivery(@RequestBody ShippedToDeliveryRequest request);

    @PostMapping("/return")
    void acceptReturn(@RequestBody Map<UUID, Long> products);
}