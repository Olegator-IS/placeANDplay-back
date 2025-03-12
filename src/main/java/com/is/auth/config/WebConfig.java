package com.is.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Настраиваем маппинг для доступа к загруженным файлам
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/domains/placeandplay.uz/public_html/uploads/");
    }
} 