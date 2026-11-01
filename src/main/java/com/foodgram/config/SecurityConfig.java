package com.foodgram.config;

import com.foodgram.repository.AuthTokenRepository;
import com.foodgram.security.ApiSecurityHandlers;
import com.foodgram.security.TokenAuthFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthTokenRepository tokenRepository)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(ApiSecurityHandlers.entryPoint())
                        .accessDeniedHandler(ApiSecurityHandlers.accessDenied()))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/token/login").permitAll()
                        // пользователи: список, регистрация и профиль по id — публичные
                        .requestMatchers(HttpMethod.GET, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/{id:\\d+}").permitAll()
                        // ингредиенты — чтение публичное
                        .requestMatchers(HttpMethod.GET, "/api/ingredients", "/api/ingredients/{id:\\d+}").permitAll()
                        // рецепты — чтение публичное (кроме списка покупок)
                        .requestMatchers(HttpMethod.GET, "/api/recipes", "/api/recipes/{id:\\d+}",
                                "/api/recipes/{id:\\d+}/get-link", "/api/recipes/{id:\\d+}/get-short-link")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/s/{hash}", "/media/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new TokenAuthFilter(tokenRepository), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("*"));
        cfg.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
