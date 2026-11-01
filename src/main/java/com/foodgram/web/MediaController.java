package com.foodgram.web;

import com.foodgram.service.ImageStorage;
import com.foodgram.web.error.ApiException;
import java.util.regex.Pattern;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Отдаёт изображения из закрытого бакета: /media/recipes/images/&lt;uuid&gt;.png и т. п.
 * Бакет не нужно делать публичным и настраивать политики — это работает с любым S3-совместимым хранилищем.
 * Имена файлов уникальны и неизменяемы, поэтому ответы кэшируются на год (и дополнительно кэшируются в nginx).
 */
@RestController
public class MediaController {

    /** Допускаются только объекты, которые создаёт само приложение. */
    private static final Pattern KEY =
            Pattern.compile("^/(?:recipes|users)/images/[0-9a-f-]{36}\\.(?:png|jpg|gif|bmp)$");

    private final ImageStorage imageStorage;

    public MediaController(ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    @GetMapping("/media/{*key}")
    public ResponseEntity<InputStreamResource> get(@PathVariable String key) {
        if (!KEY.matcher(key).matches()) {
            throw ApiException.notFound();
        }
        ResponseInputStream<GetObjectResponse> in = imageStorage.open(key.substring(1));
        GetObjectResponse meta = in.response();

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .contentType(MediaType.parseMediaType(
                        meta.contentType() != null ? meta.contentType() : "application/octet-stream"));
        if (meta.contentLength() != null) {
            builder.contentLength(meta.contentLength());
        }
        return builder.body(new InputStreamResource(in));
    }
}
