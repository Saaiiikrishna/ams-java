package com.example.attendancesystem.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

/**
 * API Gateway Application
 * Central entry point for all microservices
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes
                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri("http://auth-service:8081"))
                
                // Organization Service Routes
                .route("organization-service", r -> r.path("/api/organization/**")
                        .uri("http://organization-service:8082"))
                
                // Subscriber Service Routes
                .route("subscriber-service", r -> r.path("/api/subscriber/**")
                        .uri("http://subscriber-service:8083"))
                
                // Attendance Service Routes
                .route("attendance-service", r -> r.path("/api/attendance/**")
                        .uri("http://attendance-service:8084"))
                
                // Menu Service Routes
                .route("menu-service", r -> r.path("/api/menu/**")
                        .uri("http://menu-service:8085"))
                
                // Order Service Routes
                .route("order-service", r -> r.path("/api/order/**")
                        .uri("http://order-service:8086"))
                
                // Table Service Routes
                .route("table-service", r -> r.path("/api/table/**")
                        .uri("http://table-service:8087"))
                
                .build();
    }
}
