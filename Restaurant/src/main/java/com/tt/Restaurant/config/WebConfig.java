package com.tt.Restaurant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Existing mapping
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/D:/images/");

        // NEW mapping (optional): serve /uploads/** from a folder
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/D:/uploads/");
    }
}