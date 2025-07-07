package com.example.attendancesystem.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        System.out.println("=== REQUEST ===");
        System.out.println("Method: " + request.getMethod());
        System.out.println("URI: " + request.getURI());
        System.out.println("Headers: " + request.getHeaders());
        System.out.println("===============");
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            System.out.println("=== RESPONSE ===");
            System.out.println("Status: " + exchange.getResponse().getStatusCode());
            System.out.println("Headers: " + exchange.getResponse().getHeaders());
            System.out.println("================");
        }));
    }

    @Override
    public int getOrder() {
        return -1; // High priority
    }
}
