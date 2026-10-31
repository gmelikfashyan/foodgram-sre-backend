package com.foodgram.web.error;

import org.springframework.http.HttpStatus;

/** Ошибка с телом в формате {"detail": "..."} (как в DRF). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Страница не найдена.");
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, detail);
    }

    public static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "У вас недостаточно прав для выполнения данного действия.");
    }
}
