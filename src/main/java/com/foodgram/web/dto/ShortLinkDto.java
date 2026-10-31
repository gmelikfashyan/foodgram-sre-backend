package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShortLinkDto(@JsonProperty("short-link") String shortLink) {
}
