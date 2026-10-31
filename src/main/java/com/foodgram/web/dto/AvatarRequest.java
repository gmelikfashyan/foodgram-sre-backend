package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AvatarRequest(@JsonProperty("avatar") String avatar) {
}
