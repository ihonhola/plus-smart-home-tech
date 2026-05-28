package ru.yandex.practicum.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddressDto {
    private String country;

    private String city;

    private String street;

    private String house;

    private String flat;
}