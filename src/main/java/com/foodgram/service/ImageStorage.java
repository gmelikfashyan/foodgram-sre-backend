package com.foodgram.service;

import com.foodgram.config.AppProperties;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

/** Сохранение изображений, пришедших в base64 (data URI), в каталог media. */
@Component
public class ImageStorage {

    private static final int MAX_BYTES = 10 * 1024 * 1024;

    public record DecodedImage(byte[] bytes, String extension) {
    }

    private final Path root;
    private final String mediaUrl;

    public ImageStorage(AppProperties props) {
        this.root = Path.of(props.mediaRoot()).toAbsolutePath().normalize();
        this.mediaUrl = props.mediaUrl().endsWith("/") ? props.mediaUrl() : props.mediaUrl() + "/";
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    public Path getRoot() {
        return root;
    }

    /**
     * Декодирует base64 (с префиксом data:image/...;base64, или без него) и проверяет,
     * что это действительно изображение. Формат определяется по содержимому, а не по заголовку.
     *
     * @throws IllegalArgumentException если данные не являются корректным изображением
     */
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

    /** Сохраняет изображение и возвращает URL вида /media/&lt;subdir&gt;/&lt;file&gt;. */
    public String save(String subdir, DecodedImage image) {
        String fileName = UUID.randomUUID() + "." + image.extension();
        Path dir = root.resolve(subdir).normalize();
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), image.bytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить изображение", e);
        }
        return mediaUrl + subdir + "/" + fileName;
    }

    /** Удаляет ранее сохранённый файл по его URL (ошибки игнорируются). */
    public void delete(String url) {
        if (url == null || !url.startsWith(mediaUrl)) {
            return;
        }
        Path file = root.resolve(url.substring(mediaUrl.length())).normalize();
        if (!file.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // не критично
        }
    }
}
