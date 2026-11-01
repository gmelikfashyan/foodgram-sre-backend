package com.foodgram.service;

import com.foodgram.config.AppProperties;
import com.foodgram.web.error.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

    private static final int MAX_BYTES = 10 * 1024 * 1024;

    public record DecodedImage(byte[] bytes, String extension) {
    }

    private final S3Client s3;
    private final String bucket;
    private final String mediaUrl;

    public ImageStorage(AppProperties props, S3Client s3) {
        this.s3 = s3;
        this.bucket = props.storage().bucket();
        this.mediaUrl = props.mediaUrl().endsWith("/") ? props.mediaUrl() : props.mediaUrl() + "/";
    }

    public DecodedImage decode(String data) {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Пустое изображение.");
        }
        String payload = data.trim();
        int marker = payload.indexOf("base64,");
        if (payload.startsWith("data:") && marker >= 0) {
            payload = payload.substring(marker + "base64,".length());
        }
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректная кодировка base64.");
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("Недопустимый размер изображения.");
        }
        String format = detectFormat(bytes);
        if (format == null) {
            throw new IllegalArgumentException("Некорректный формат переданного файла.");
        }
        return new DecodedImage(bytes, "jpeg".equals(format) ? "jpg" : format);
    }

    private static String detectFormat(byte[] bytes) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                reader.getWidth(0); // убеждаемся, что заголовок читается
                String name = reader.getFormatName().toLowerCase();
                return switch (name) {
                    case "png", "gif", "jpeg", "bmp" -> name;
                    default -> null;
                };
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }


    public String save(String subdir, DecodedImage image) {
        String key = subdir + "/" + UUID.randomUUID() + "." + image.extension();
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType(image.extension()))
                        .build(),
                RequestBody.fromBytes(image.bytes()));
        return mediaUrl + key;
    }

    public ResponseInputStream<GetObjectResponse> open(String key) {
        try {
            return s3.getObject(b -> b.bucket(bucket).key(key));
        } catch (NoSuchKeyException e) {
            throw ApiException.notFound();
        }
    }

    public void delete(String url) {
        if (url == null || !url.startsWith(mediaUrl)) {
            return;
        }
        String key = url.substring(mediaUrl.length());
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(key));
        } catch (SdkException e) {
            log.warn("Не удалось удалить объект {}: {}", key, e.getMessage());
        }
    }

    private static String contentType(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            default -> "image/jpeg";
        };
    }
}
