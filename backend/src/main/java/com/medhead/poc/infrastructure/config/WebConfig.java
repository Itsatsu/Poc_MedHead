package com.medhead.poc.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configure le CORS pour autoriser le frontend React (servi sur une origine distincte,
 * ex. Vite en dev) à appeler l'API {@code /api/**} depuis le navigateur. Les origines
 * autorisées sont externalisées en configuration ({@code medhead.cors.allowed-origins})
 * plutôt que codées en dur, pour s'adapter aux environnements (dev/prod).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebConfig(@Value("${medhead.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST");
    }
}
