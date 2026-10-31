package com.foodgram.security;

import com.foodgram.repository.AuthTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Аутентификация по заголовку {@code Authorization: Token <key>} (формат DRF TokenAuthentication).
 * В качестве principal кладётся сущность {@link com.foodgram.domain.User}, в details — ключ токена.
 */
public class TokenAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "token ";

    private final AuthTokenRepository tokenRepository;

    public TokenAuthFilter(AuthTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String key = header.substring(PREFIX.length()).trim();
            if (!key.isEmpty()) {
                tokenRepository.findWithUserByKey(key).ifPresent(token -> {
                    var auth = UsernamePasswordAuthenticationToken.authenticated(
                            token.getUser(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    auth.setDetails(token.getKey());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        }
        chain.doFilter(request, response);
    }
}
