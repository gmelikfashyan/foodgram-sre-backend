package com.foodgram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String mediaRoot,
        String mediaUrl,
        String hashidsSalt,
        int pageSize,
        int maxPageSize
) {
}
