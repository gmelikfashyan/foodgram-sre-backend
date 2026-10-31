package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SetPasswordRequest(
        @JsonProperty("current_password") String currentPassword,
        @JsonProperty("new_password") String newPassword
) {
}
