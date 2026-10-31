package com.foodgram.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PageDto<T>(
        @JsonProperty("count") long count,
        @JsonProperty("next") String next,
        @JsonProperty("previous") String previous,
        @JsonProperty("results") List<T> results
) {
}
