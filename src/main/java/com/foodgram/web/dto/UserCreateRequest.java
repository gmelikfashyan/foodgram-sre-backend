package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserCreateRequest(
        @JsonProperty("email") String email,
        @JsonProperty("username") String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("password") String password
) {
}
