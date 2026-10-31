package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IngredientAmountRequest(
        @JsonProperty("id") Long id,
        @JsonProperty("amount") Integer amount
) {
}
