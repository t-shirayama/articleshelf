package com.articleshelf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String frontendOrigin;
    private final String extensionOrigins;

    public CorsConfig(
            @Value("${articleshelf.frontend-origin}") String frontendOrigin,
            @Value("${articleshelf.extension-auth.allowed-origins:}") String extensionOrigins
    ) {
        this.frontendOrigin = frontendOrigin;
        this.extensionOrigins = extensionOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins())
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-CSRF-Token")
                .allowCredentials(true);
    }

    private String[] allowedOrigins() {
        String[] origins = Arrays.stream((frontendOrigin + "," + extensionOrigins).split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toArray(String[]::new);
        for (String origin : origins) {
            if (origin.contains("*")) {
                throw new IllegalStateException("CORS allowed origins must be explicit. Wildcards are not allowed.");
            }
        }
        return origins;
    }
}
