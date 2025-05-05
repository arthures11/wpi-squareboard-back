package com.bryja.wpisquareboardback.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Apply CORS to paths starting with /api/
                        // IMPORTANT: For development, allow your Angular origin
                        .allowedOrigins("http://localhost:4200")
                        // For production, replace with your actual frontend domain(s):
                        // .allowedOrigins("https://your-frontend-domain.com", "https://www.your-frontend-domain.com")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // Allowed HTTP methods
                        .allowedHeaders("*") // Allow all headers (can be restricted)
                        .allowCredentials(true) // If you need cookies/session support (often not needed for stateless APIs)
                        .maxAge(3600); // Cache preflight response for 1 hour
            }
        };
    }
}
