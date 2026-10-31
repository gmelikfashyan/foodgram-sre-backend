package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RecipeDto(
        @JsonProperty("id") Long id,
        @JsonProperty("author") UserDto author,
        @JsonProperty("ingredients") List<RecipeIngredientDto> ingredients,
        @JsonProperty("is_favorited") boolean favorited,
        @JsonProperty("is_in_shopping_cart") boolean inShoppingCart,
        @JsonProperty("name") String name,
        @JsonProperty("image") String image,
        @JsonProperty("text") String text,
        @JsonProperty("cooking_time") Integer cookingTime
) {
}
