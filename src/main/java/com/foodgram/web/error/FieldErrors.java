package com.foodgram.web.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FieldErrors {

    public static final String REQUIRED = "Обязательное поле.";

    private final Map<String, Object> map = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public FieldErrors add(String field, String message) {
        ((List<String>) map.computeIfAbsent(field, k -> new ArrayList<String>())).add(message);
        return this;
    }

    public FieldErrors put(String field, Object value) {
        map.put(field, value);
        return this;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void throwIfAny() {
        if (!map.isEmpty()) {
            throw new ValidationException(map);
        }
    }

    public static ValidationException single(String field, String message) {
        return new ValidationException(Map.of(field, List.of(message)));
    }
}
