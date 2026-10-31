package com.foodgram.web.error;

import java.util.Map;

/** Ошибки валидации в формате {"поле": ["сообщение", ...]} (как в DRF). */
public class ValidationException extends RuntimeException {

    private final transient Map<String, Object> errors;

    public ValidationException(Map<String, ?> errors) {
        super("Validation failed");
        this.errors = new java.util.LinkedHashMap<>(errors);
    }

    public Map<String, Object> getErrors() {
        return errors;
    }
}
