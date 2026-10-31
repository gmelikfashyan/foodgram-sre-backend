package com.foodgram.web;

import com.foodgram.config.AppProperties;
import com.foodgram.web.dto.PageDto;
import com.foodgram.web.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Параметры page/limit и формат ответа {count, next, previous, results} как у DRF PageNumberPagination. */
@Component
public class Paging {

    private final AppProperties props;

    public Paging(AppProperties props) {
        this.props = props;
    }

    public int page(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            int page = Integer.parseInt(raw.trim());
            if (page < 1) {
                throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Invalid page.");
            }
            return page;
        } catch (NumberFormatException e) {
            throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Invalid page.");
        }
    }

    public int limit(String raw) {
        if (raw == null || raw.isBlank()) {
            return props.pageSize();
        }
        try {
            int limit = Integer.parseInt(raw.trim());
            return limit < 1 ? props.pageSize() : Math.min(limit, props.maxPageSize());
        } catch (NumberFormatException e) {
            return props.pageSize();
        }
    }

    public <T> PageDto<T> toDto(Page<T> page, int pageNumber, HttpServletRequest request) {
        if (pageNumber > 1 && pageNumber > page.getTotalPages()) {
            throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Invalid page.");
        }
        String next = page.hasNext() ? link(request, pageNumber + 1) : null;
        String previous = page.hasPrevious() ? link(request, pageNumber - 1) : null;
        return new PageDto<>(page.getTotalElements(), next, previous, page.getContent());
    }

    public <S, T> PageDto<T> toDto(Page<S> page, Function<List<S>, List<T>> mapper, int pageNumber,
                                   HttpServletRequest request) {
        PageDto<S> raw = toDto(page, pageNumber, request);
        return new PageDto<>(raw.count(), raw.next(), raw.previous(), mapper.apply(raw.results()));
    }

    private static String link(HttpServletRequest request, int page) {
        return ServletUriComponentsBuilder.fromRequest(request)
                .replaceQueryParam("page", page)
                .build()
                .toUriString();
    }

    /** "1", "true" (без учёта регистра) — истина; всё остальное — ложь. */
    public static boolean flag(String raw) {
        return raw != null && ("1".equals(raw.trim()) || "true".equalsIgnoreCase(raw.trim()));
    }
}
