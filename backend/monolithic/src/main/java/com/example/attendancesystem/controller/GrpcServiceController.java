package com.example.attendancesystem.subscriber.controller;

import com.example.attendancesystem.config.GrpcClientConfig;
import com.example.attendancesystem.service.GrpcServiceFacade;
import com.example.attendancesystem.service.GrpcClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for monitoring and managing gRPC services
 * Provides endpoints for checking service health, availability, and connection status
 */
@RestController
@RequestMapping("/api/grpc")
public class GrpcServiceController {

    @Autowired
    private GrpcServiceFacade grpcServiceFacade;

    @Autowired
    private GrpcClientConfig grpcClientConfig;

    @Autowired
    private GrpcClientFactory grpcClientFactory;

    /**
     * Get overall gRPC services health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getServicesHealth() {
        Map<String, Object> health = grpcServiceFacade.getServiceHealth();
        health.put("timestamp", System.currentTimeMillis());
        health.put("status", grpcServiceFacade.areAllCriticalServicesAvailable() ? "HEALTHY" : "DEGRADED");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Get detailed service availability
     */
    @GetMapping("/availability")
    public ResponseEntity<Map<String, Object>> getServiceAvailability() {
        Map<String, Object> response = new HashMap<>();
        response.put("serviceAvailability", grpcClientConfig.getServiceAvailability());
        response.put("unavailableServices", grpcServiceFacade.getUnavailableServices());
        response.put("allServicesAvailable", grpcClientConfig.areAllServicesAvailable());
        response.put("criticalServicesAvailable", grpcServiceFacade.areAllCriticalServicesAvailable());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a specific service is available
     */
    @GetMapping("/availability/{serviceName}")
    public ResponseEntity<Map<String, Object>> checkServiceAvailability(@PathVariable String serviceName) {
        Map<String, Object> response = new HashMap<>();
        boolean available = grpcServiceFacade.isServiceAvailable(serviceName);
        
        response.put("serviceName", serviceName);
        response.put("available", available);
        response.put("timestamp", System.currentTimeMillis());
        
        if (available) {
            response.put("status", "AVAILABLE");
            response.put("message", "Service is available and ready");
        } else {
            response.put("status", "UNAVAILABLE");
            response.put("message", "Service is not available or not discovered");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get gRPC connection statistics
     */
    @GetMapping("/connections")
    public ResponseEntity<Map<String, Object>> getConnectionStatistics() {
        Map<String, Object> stats = grpcClientFactory.getConnectionStatistics();
        stats.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Refresh all gRPC connections
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshConnections() {
        try {
            grpcClientConfig.refreshAllConnections();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All gRPC connections refreshed successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to refresh connections: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get service discovery status
     */
    @GetMapping("/discovery")
    public ResponseEntity<Map<String, Object>> getServiceDiscoveryStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, javax.jmdns.ServiceInfo> discoveredServices = grpcClientFactory.getAvailableServices();
            
            response.put("discoveredServices", discoveredServices.size());
            response.put("serviceNames", discoveredServices.keySet());
            response.put("discoveryActive", true);
            response.put("timestamp", System.currentTimeMillis());
            
            // Add detailed service info
            Map<String, Object> serviceDetails = new HashMap<>();
            discoveredServices.forEach((name, info) -> {
                Map<String, Object> details = new HashMap<>();
                details.put("hostAddresses", info.getHostAddresses());
                details.put("port", info.getPort());
                details.put("type", info.getType());
                serviceDetails.put(name, details);
            });
            response.put("serviceDetails", serviceDetails);
            
        } catch (Exception e) {
            response.put("discoveredServices", 0);
            response.put("discoveryActive", false);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test connectivity to all services
     */
    @PostMapping("/test-connectivity")
    public ResponseEntity<Map<String, Object>> testConnectivity() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> testResults = new HashMap<>();
        
        // Test each service
        String[] services = {"auth", "organization", "subscriber", "attendance", "menu", "order", "table"};
        
        for (String service : services) {
            Map<String, Object> result = new HashMap<>();
            try {
                boolean available = grpcServiceFacade.isServiceAvailable(service);
                result.put("available", available);
                result.put("status", available ? "CONNECTED" : "DISCONNECTED");
                result.put("error", null);
            } catch (Exception e) {
                result.put("available", false);
                result.put("status", "ERROR");
                result.put("error", e.getMessage());
            }
            testResults.put(service + "-service", result);
        }
        
        response.put("testResults", testResults);
        response.put("timestamp", System.currentTimeMillis());
        
        long connectedServices = testResults.values().stream()
                .mapToLong(result -> {
                    Map<String, Object> r = (Map<String, Object>) result;
                    return (Boolean) r.get("available") ? 1 : 0;
                })
                .sum();
        
        response.put("connectedServices", connectedServices);
        response.put("totalServices", services.length);
        response.put("connectivityPercentage", (connectedServices * 100.0) / services.length);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get gRPC service configuration
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getGrpcConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("serviceDiscoveryEnabled", true);
        config.put("grpcPort", 9090);
        config.put("maxInboundMessageSize", "4MB");
        config.put("keepAliveTime", "30 seconds");
        config.put("keepAliveTimeout", "5 seconds");
        config.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(config);
    }
}
