package com.foodgram.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;


public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // проверки работоспособности не засоряют журнал
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || !SAFE_ID.matcher(requestId).matches()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("request_id", requestId);
        response.setHeader("X-Request-Id", requestId);

        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            MDC.put("http_method", request.getMethod());
            MDC.put("http_path", request.getRequestURI());
            MDC.put("http_status", String.valueOf(status));
            MDC.put("duration_ms", String.valueOf((System.nanoTime() - start) / 1_000_000));
            if (status >= 500) {
                log.error("{} {} -> {}", request.getMethod(), request.getRequestURI(), status);
            } else {
                log.info("{} {} -> {}", request.getMethod(), request.getRequestURI(), status);
            }
            MDC.clear();
        }
    }
}
