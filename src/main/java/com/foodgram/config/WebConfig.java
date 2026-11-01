package com.foodgram.config;

import com.foodgram.logging.RequestLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class WebConfig {

    @Bean
    FilterRegistrationBean<TrailingSlashFilter> trailingSlashFilter() {
        FilterRegistrationBean<TrailingSlashFilter> bean = new FilterRegistrationBean<>(new TrailingSlashFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> bean = new FilterRegistrationBean<>(new RequestLoggingFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return bean;
    }

    static class TrailingSlashFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            String uri = request.getRequestURI();
            if (uri.length() > 1 && uri.endsWith("/")) {
                chain.doFilter(new HttpServletRequestWrapper(request) {
                    @Override
                    public String getRequestURI() {
                        return strip(super.getRequestURI());
                    }

                    @Override
                    public String getServletPath() {
                        return strip(super.getServletPath());
                    }
                }, response);
            } else {
                chain.doFilter(request, response);
            }
        }

        private static String strip(String path) {
            return (path != null && path.length() > 1 && path.endsWith("/"))
                    ? path.substring(0, path.length() - 1) : path;
        }
    }
}
