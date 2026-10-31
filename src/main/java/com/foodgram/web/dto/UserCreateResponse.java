package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserCreateResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("email") String email
) {
}
