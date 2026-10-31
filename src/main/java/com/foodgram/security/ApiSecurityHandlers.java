package com.foodgram.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** JSON-ответы 401/403 в формате DRF: {"detail": "..."}. */
public final class ApiSecurityHandlers {

    private ApiSecurityHandlers() {
    }

    public static AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) ->
                write(response, HttpStatus.UNAUTHORIZED, "Учетные данные не были предоставлены.");
    }

    public static AccessDeniedHandler accessDenied() {
        return (request, response, ex) ->
                write(response, HttpStatus.FORBIDDEN, "У вас недостаточно прав для выполнения данного действия.");
    }

    private static void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"detail\":\"" + detail + "\"}");
    }
}
