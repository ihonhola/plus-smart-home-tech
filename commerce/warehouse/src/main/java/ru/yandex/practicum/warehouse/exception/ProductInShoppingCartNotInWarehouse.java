package ru.yandex.practicum.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ProductInShoppingCartNotInWarehouse extends RuntimeException {
    public ProductInShoppingCartNotInWarehouse(String message) {
        super(message);
    }
}
