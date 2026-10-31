package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UserWithRecipesDto(
        @JsonProperty("email") String email,
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("is_subscribed") boolean subscribed,
        @JsonProperty("recipes") List<RecipeShortDto> recipes,
        @JsonProperty("recipes_count") long recipesCount,
        @JsonProperty("avatar") String avatar
) {
}
