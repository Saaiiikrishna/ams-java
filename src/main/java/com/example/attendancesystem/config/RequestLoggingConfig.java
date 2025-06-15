package com.example.attendancesystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class RequestLoggingConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingConfig.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor());
    }

    public static class RequestLoggingInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            logger.info("=== INCOMING REQUEST ===");
            logger.info("Method: {}", request.getMethod());
            logger.info("URI: {}", request.getRequestURI());
            logger.info("Query String: {}", request.getQueryString());
            logger.info("Remote Address: {}", request.getRemoteAddr());
            logger.info("User Agent: {}", request.getHeader("User-Agent"));
            logger.info("Referer: {}", request.getHeader("Referer"));
            logger.info("Accept: {}", request.getHeader("Accept"));
            logger.info("Content Type: {}", request.getContentType());
            logger.info("Handler: {}", handler);
            logger.info("========================");
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            logger.info("=== REQUEST COMPLETED ===");
            logger.info("URI: {}", request.getRequestURI());
            logger.info("Response Status: {}", response.getStatus());
            logger.info("Content Type: {}", response.getContentType());
            if (ex != null) {
                logger.error("Exception occurred: ", ex);
            }
            logger.info("=========================");
        }
    }
}
