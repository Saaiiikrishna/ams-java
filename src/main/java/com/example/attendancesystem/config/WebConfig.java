package com.example.attendancesystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve general static resources (images, favicon, etc.)
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/assets/")
                .setCachePeriod(3600);
    }
}
