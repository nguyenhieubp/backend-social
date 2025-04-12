package com.example.manager.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH","DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("*") // Thêm để cho phép các header được trả về
                        .maxAge(3600); // Thời gian tồn tại của CORS (tính bằng giây)
            }
        };
    }
}
