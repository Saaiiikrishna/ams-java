package com.example.attendancesystem.subscriber.controller;

import com.example.attendancesystem.service.ServiceRegistryManager;
import com.example.attendancesystem.service.ServiceRegistryManager.ServiceInstance;
import com.example.attendancesystem.service.ServiceRegistryManager.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Service Registry Management
 * Provides endpoints for service registration, discovery, and monitoring
 */
@RestController
@RequestMapping("/api/service-registry")
@CrossOrigin(origins = "*")
public class ServiceRegistryController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryController.class);

    @Autowired
    private ServiceRegistryManager serviceRegistryManager;

    /**
     * Register a new service instance
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerService(@RequestBody ServiceRegistrationRequest request) {
        try {
            ServiceInstance instance = new ServiceInstance(
                request.getId(),
                request.getName(),
                request.getVersion(),
                request.getHost(),
                request.getPort()
            );
            
            // Add custom metadata
            if (request.getMetadata() != null) {
                instance.getMetadata().putAll(request.getMetadata());
            }
            
            serviceRegistryManager.registerService(instance);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Service registered successfully");
            response.put("instanceId", instance.getId());
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("✅ Service registered via API: {}", instance.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to register service: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Unregister a service instance
     */
    @DeleteMapping("/unregister/{instanceId}")
    public ResponseEntity<Map<String, Object>> unregisterService(@PathVariable String instanceId) {
        try {
            serviceRegistryManager.unregisterService(instanceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Service unregistered successfully");
            response.put("instanceId", instanceId);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("✅ Service unregistered via API: {}", instanceId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to unregister service: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update service heartbeat
     */
    @PostMapping("/heartbeat/{instanceId}")
    public ResponseEntity<Map<String, Object>> updateHeartbeat(@PathVariable String instanceId) {
        try {
            serviceRegistryManager.updateHeartbeat(instanceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Heartbeat updated");
            response.put("instanceId", instanceId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to update heartbeat: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all healthy service instances
     */
    @GetMapping("/instances")
    public ResponseEntity<Map<String, Object>> getHealthyInstances() {
        try {
            List<ServiceInstance> instances = serviceRegistryManager.getHealthyInstances();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("instances", instances);
            response.put("count", instances.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get service instances: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get service instances by version
     */
    @GetMapping("/instances/version/{version}")
    public ResponseEntity<Map<String, Object>> getInstancesByVersion(@PathVariable String version) {
        try {
            List<ServiceInstance> instances = serviceRegistryManager.getInstancesByVersion(version);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("instances", instances);
            response.put("version", version);
            response.put("count", instances.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get service instances by version: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get best service instance for load balancing
     */
    @GetMapping("/instances/best")
    public ResponseEntity<Map<String, Object>> getBestInstance() {
        try {
            ServiceInstance instance = serviceRegistryManager.getBestInstance();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("instance", instance);
            response.put("timestamp", System.currentTimeMillis());
            
            if (instance == null) {
                response.put("message", "No healthy instances available");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get best service instance: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get service registry statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRegistryStats() {
        try {
            Map<String, Object> stats = serviceRegistryManager.getRegistryStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get registry stats: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update service health status
     */
    @PostMapping("/health/{instanceId}")
    public ResponseEntity<Map<String, Object>> updateServiceHealth(
            @PathVariable String instanceId,
            @RequestBody ServiceHealthRequest request) {
        try {
            ServiceStatus status = ServiceStatus.valueOf(request.getStatus().toUpperCase());
            serviceRegistryManager.updateServiceHealth(instanceId, status, request.getResponseTime());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Service health updated");
            response.put("instanceId", instanceId);
            response.put("status", status);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to update service health: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Test service failover scenario
     */
    @PostMapping("/test/failover")
    public ResponseEntity<Map<String, Object>> testFailover(@RequestBody FailoverTestRequest request) {
        try {
            // Simulate failover by marking specified instances as unhealthy
            for (String instanceId : request.getInstanceIds()) {
                serviceRegistryManager.updateServiceHealth(instanceId, ServiceStatus.UNHEALTHY, 0);
            }
            
            // Get the best available instance after failover
            ServiceInstance bestInstance = serviceRegistryManager.getBestInstance();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Failover test completed");
            response.put("failedInstances", request.getInstanceIds());
            response.put("bestAvailableInstance", bestInstance);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("🧪 Failover test completed: {} instances failed, best available: {}", 
                       request.getInstanceIds().size(), 
                       bestInstance != null ? bestInstance.getId() : "none");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to test failover: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Request DTOs
    public static class ServiceRegistrationRequest {
        private String id;
        private String name;
        private String version;
        private String host;
        private int port;
        private Map<String, String> metadata;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }

    public static class ServiceHealthRequest {
        private String status;
        private long responseTime;

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    }

    public static class FailoverTestRequest {
        private List<String> instanceIds;

        // Getters and setters
        public List<String> getInstanceIds() { return instanceIds; }
        public void setInstanceIds(List<String> instanceIds) { this.instanceIds = instanceIds; }
    }
}
