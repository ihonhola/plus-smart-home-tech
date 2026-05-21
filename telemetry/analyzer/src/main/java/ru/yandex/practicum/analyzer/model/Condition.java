package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.Column;
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

@Entity
@Table(name = "conditions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;   // ConditionType (MOTION, LUMINOSITY, ...)

    @Column(nullable = false)
    private String operation; // ConditionOperation (EQUALS, GREATER_THAN, LOWER_THAN)

    private Integer value;  // может быть null для некоторых условий
}