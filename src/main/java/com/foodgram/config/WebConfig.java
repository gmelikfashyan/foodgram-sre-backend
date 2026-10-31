package com.foodgram.config;

import com.foodgram.service.ImageStorage;
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
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ImageStorage imageStorage;
    private final AppProperties props;

    public WebConfig(ImageStorage imageStorage, AppProperties props) {
        this.imageStorage = imageStorage;
        this.props = props;
    }

    /** Раздача загруженных изображений по /media/** (на проде этим занимается nginx). */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = imageStorage.getRoot().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        String url = props.mediaUrl().endsWith("/") ? props.mediaUrl() : props.mediaUrl() + "/";
        registry.addResourceHandler(url + "**").addResourceLocations(location);
    }

    /**
     * Фронтенд и спецификация API используют адреса со слэшем на конце (/api/users/me/),
     * а Spring 6 их по умолчанию не сопоставляет. Фильтр прозрачно убирает завершающий слэш
     * до Spring Security и DispatcherServlet.
     */
    @Bean
    FilterRegistrationBean<TrailingSlashFilter> trailingSlashFilter() {
        FilterRegistrationBean<TrailingSlashFilter> bean = new FilterRegistrationBean<>(new TrailingSlashFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
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
