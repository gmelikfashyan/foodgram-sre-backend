package com.foodgram.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;


public class JsonLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String serviceName;
    private final String serviceVersion;

    public JsonLogFormatter(Environment environment) {
        this.serviceName = environment.getProperty("spring.application.name", "foodgram");
        this.serviceVersion = environment.getProperty("info.app.version", "unknown");
    }

    @Override
    public String format(ILoggingEvent event) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(event.getInstant()));
        json.put("level", event.getLevel().toString());
        json.put("message", event.getFormattedMessage());
        json.put("service.name", serviceName);
        json.put("service.version", serviceVersion);
        json.put("logger", event.getLoggerName());
        json.put("thread", event.getThreadName());

        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            mdc.forEach(json::putIfAbsent);
        }

        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null) {
            json.put("error.type", throwable.getClassName());
            json.put("error.message", throwable.getMessage());
            json.put("error.stack_trace", ThrowableProxyUtil.asString(throwable));
        }

        try {
            return MAPPER.writeValueAsString(json) + "\n";
        } catch (JsonProcessingException e) {
            // Запасной вариант, чтобы событие не потерялось
            return "{\"timestamp\":\"" + DateTimeFormatter.ISO_INSTANT.format(event.getInstant())
                    + "\",\"level\":\"ERROR\",\"message\":\"log serialization failed\",\"service.name\":\""
                    + serviceName + "\"}\n";
        }
    }
}
