package com.foodgram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "info")
public record AppProperties(
        String mediaUrl,
        String hashidsSalt,
        int pageSize,
        int maxPageSize,
        Storage storage
) {

    public record Storage(
            String endpoint,
            String region,
            String bucket,
            String accessKey,
            String secretKey,
            boolean pathStyle,
            boolean createBucket
    ) {
    }
}
