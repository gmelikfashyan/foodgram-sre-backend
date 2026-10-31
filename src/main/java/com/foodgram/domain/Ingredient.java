package com.foodgram.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "measurement_unit", nullable = false, length = 64)
    private String measurementUnit;

    protected Ingredient() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getMeasurementUnit() { return measurementUnit; }
}
