package ru.yandex.practicum.delivery.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressEmbeddable {
    private String country;

    private String city;

    private String street;

    private String house;

    private String flat;
}