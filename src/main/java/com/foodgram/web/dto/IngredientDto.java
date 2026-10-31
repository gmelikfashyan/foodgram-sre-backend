package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IngredientDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("measurement_unit") String measurementUnit
) {
}
