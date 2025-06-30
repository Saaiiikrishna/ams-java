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

        // Serve React Admin Panel from backend
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/")
                .setCachePeriod(3600);

        // Serve React Entity Dashboard from backend
        registry.addResourceHandler("/entity/**")
                .addResourceLocations("classpath:/static/entity/")
                .setCachePeriod(3600);

        // Serve static resources for React apps (JS, CSS, etc.)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/admin/static/", "classpath:/static/entity/static/")
                .setCachePeriod(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward React Router routes to index.html for admin panel
        registry.addViewController("/admin").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html");

        // Forward React Router routes to index.html for entity dashboard
        registry.addViewController("/entity").setViewName("forward:/entity/index.html");
        registry.addViewController("/entity/").setViewName("forward:/entity/index.html");
    }
}
