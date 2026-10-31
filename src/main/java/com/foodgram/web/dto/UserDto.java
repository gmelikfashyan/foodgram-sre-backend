package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDto(
        @JsonProperty("email") String email,
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("is_subscribed") boolean subscribed,
        @JsonProperty("avatar") String avatar
) {
}
