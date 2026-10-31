package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RecipeIngredientDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("measurement_unit") String measurementUnit,
        @JsonProperty("amount") Integer amount
) {
}
