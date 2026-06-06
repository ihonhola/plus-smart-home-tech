package ru.yandex.practicum.warehouse.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "warehouse_products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WarehouseProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId; // ссылка на товар в shopping-store

    @Column(nullable = false)
    private boolean fragile;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "width", column = @Column(name = "width")),
            @AttributeOverride(name = "height", column = @Column(name = "height")),
            @AttributeOverride(name = "depth", column = @Column(name = "depth"))
    })
    private Dimension dimension;

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false)
    private long quantity; // количество на складе

    @Embeddable
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Dimension {
        private double width;
        private double height;
        private double depth;
    }
}