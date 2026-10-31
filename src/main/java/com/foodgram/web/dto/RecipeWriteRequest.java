package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Тело запроса создания/обновления рецепта. {@code cooking_time} принимается как «сырой» JSON,
 * чтобы отличать отсутствующее поле от пустой строки (пустая строка — ошибка валидации).
 */
public record RecipeWriteRequest(
        @JsonProperty("name") String name,
        @JsonProperty("text") String text,
        @JsonProperty("cooking_time") JsonNode cookingTime,
        @JsonProperty("image") String image,
        @JsonProperty("ingredients") List<IngredientAmountRequest> ingredients
) {
}
