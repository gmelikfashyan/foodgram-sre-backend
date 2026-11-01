package com.foodgram.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    private final AppProperties.Storage storage;

    public StorageConfig(AppProperties props) {
        this.storage = props.storage();
    }

    @Bean(destroyMethod = "close")
    S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(storage.endpoint()))
                .region(Region.of(storage.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(storage.accessKey(), storage.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storage.pathStyle())
                        .build())
                .build();
    }

    @Bean
    BucketInitializer bucketInitializer(S3Client s3) {
        return new BucketInitializer(s3, storage);
    }

    static class BucketInitializer {

        private final S3Client s3;
        private final AppProperties.Storage storage;

        BucketInitializer(S3Client s3, AppProperties.Storage storage) {
            this.s3 = s3;
            this.storage = storage;
        }

        @PostConstruct
        void ensureBucket() {
            try {
                s3.headBucket(b -> b.bucket(storage.bucket()));
            } catch (S3Exception e) {
                if (e.statusCode() == 404 && storage.createBucket()) {
                    log.info("Бакет {} не найден, создаю", storage.bucket());
                    s3.createBucket(b -> b.bucket(storage.bucket()));
                } else {
                    throw e;
                }
            }
        }
    }
}
