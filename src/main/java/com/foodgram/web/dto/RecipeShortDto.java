package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RecipeShortDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("image") String image,
        @JsonProperty("cooking_time") Integer cookingTime
) {
}
