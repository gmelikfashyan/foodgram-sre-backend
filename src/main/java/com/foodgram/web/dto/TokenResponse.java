package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(@JsonProperty("auth_token") String authToken) {
}
