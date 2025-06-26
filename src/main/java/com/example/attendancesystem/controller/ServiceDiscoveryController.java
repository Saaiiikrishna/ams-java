package com.example.attendancesystem.controller;

import com.example.attendancesystem.service.ServiceDiscoveryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.jmdns.ServiceInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Service Discovery operations
 * Provides endpoints for monitoring and managing service discovery
 */
@RestController
@RequestMapping("/api/service-discovery")
public class ServiceDiscoveryController {

    @Autowired
    private ServiceDiscoveryManager serviceDiscoveryManager;

    /**
     * Get service discovery health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = serviceDiscoveryManager.getServiceHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Get all discovered services
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getAllServices() {
        Map<String, ServiceInfo> services = serviceDiscoveryManager.getAllDiscoveredServices();
        Map<String, Object> response = new HashMap<>();
        
        response.put("count", services.size());
        response.put("services", services.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertServiceInfoToMap(entry.getValue())
                )));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Find a specific service by name
     */
    @GetMapping("/services/{serviceName}")
    public ResponseEntity<Map<String, Object>> getService(@PathVariable String serviceName) {
        Optional<ServiceInfo> serviceOpt = serviceDiscoveryManager.findService(serviceName);
        
        if (serviceOpt.isPresent()) {
            Map<String, Object> response = convertServiceInfoToMap(serviceOpt.get());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Service not found");
            error.put("serviceName", serviceName);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get service endpoint URL
     */
    @GetMapping("/services/{serviceName}/endpoint")
    public ResponseEntity<Map<String, Object>> getServiceEndpoint(@PathVariable String serviceName) {
        Optional<String> endpointOpt = serviceDiscoveryManager.getServiceEndpoint(serviceName);
        
        Map<String, Object> response = new HashMap<>();
        if (endpointOpt.isPresent()) {
            response.put("serviceName", serviceName);
            response.put("endpoint", endpointOpt.get());
            
            // Also get gRPC endpoint if available
            Optional<String> grpcEndpointOpt = serviceDiscoveryManager.getGrpcServiceEndpoint(serviceName);
            if (grpcEndpointOpt.isPresent()) {
                response.put("grpcEndpoint", grpcEndpointOpt.get());
            }
            
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Service endpoint not found");
            response.put("serviceName", serviceName);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Discover services by type
     */
    @GetMapping("/discover")
    public ResponseEntity<Map<String, Object>> discoverServices(@RequestParam String serviceType) {
        List<ServiceInfo> services = serviceDiscoveryManager.discoverServices(serviceType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("serviceType", serviceType);
        response.put("count", services.size());
        response.put("services", services.stream()
                .map(this::convertServiceInfoToMap)
                .toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get services by environment
     */
    @GetMapping("/services/environment/{environment}")
    public ResponseEntity<Map<String, Object>> getServicesByEnvironment(@PathVariable String environment) {
        List<ServiceInfo> services = serviceDiscoveryManager.findServicesByEnvironment(environment);
        
        Map<String, Object> response = new HashMap<>();
        response.put("environment", environment);
        response.put("count", services.size());
        response.put("services", services.stream()
                .map(this::convertServiceInfoToMap)
                .toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get services by version
     */
    @GetMapping("/services/version/{version}")
    public ResponseEntity<Map<String, Object>> getServicesByVersion(@PathVariable String version) {
        List<ServiceInfo> services = serviceDiscoveryManager.findServicesByVersion(version);
        
        Map<String, Object> response = new HashMap<>();
        response.put("version", version);
        response.put("count", services.size());
        response.put("services", services.stream()
                .map(this::convertServiceInfoToMap)
                .toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get service discovery statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = serviceDiscoveryManager.getServiceStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get our own service information
     */
    @GetMapping("/self")
    public ResponseEntity<Map<String, Object>> getOurServiceInfo() {
        Optional<ServiceInfo> serviceOpt = serviceDiscoveryManager.getOurServiceInfo();
        
        if (serviceOpt.isPresent()) {
            Map<String, Object> response = convertServiceInfoToMap(serviceOpt.get());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Service not registered");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Refresh service discovery cache
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCache() {
        serviceDiscoveryManager.refreshCache();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Service discovery cache refreshed");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if service discovery is running
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", serviceDiscoveryManager.isRunning());
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }

    /**
     * Convert ServiceInfo to a Map for JSON serialization
     */
    private Map<String, Object> convertServiceInfoToMap(ServiceInfo serviceInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", serviceInfo.getName());
        map.put("type", serviceInfo.getType());
        map.put("port", serviceInfo.getPort());
        map.put("weight", serviceInfo.getWeight());
        map.put("priority", serviceInfo.getPriority());
        map.put("hostAddresses", serviceInfo.getHostAddresses());
        map.put("server", serviceInfo.getServer());
        
        // Add properties
        Map<String, String> properties = new HashMap<>();
        for (String key : serviceInfo.getPropertyNames()) {
            properties.put(key, serviceInfo.getPropertyString(key));
        }
        map.put("properties", properties);
        
        return map;
    }
}
