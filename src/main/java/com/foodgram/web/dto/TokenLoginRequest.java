package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenLoginRequest(
        @JsonProperty("email") String email,
        @JsonProperty("password") String password
) {
}
