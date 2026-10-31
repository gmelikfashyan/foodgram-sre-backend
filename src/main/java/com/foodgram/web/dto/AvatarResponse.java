package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AvatarResponse(@JsonProperty("avatar") String avatar) {
}
