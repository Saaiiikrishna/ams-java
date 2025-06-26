package com.example.attendancesystem.subscriber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service Registry Manager for handling service metadata, versioning, and failover scenarios
 * Provides centralized service discovery and health monitoring capabilities
 */
@Service
public class ServiceRegistryManager {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryManager.class);

    @Value("${service.discovery.service-name:attendance-system}")
    private String serviceName;

    @Value("${service.discovery.version:1.0.0}")
    private String serviceVersion;

    @Value("${service.discovery.environment:development}")
    private String environment;

    @Value("${service.discovery.region:local}")
    private String region;

    // Service instance data
    public static class ServiceInstance {
        private String id;
        private String name;
        private String version;
        private String host;
        private int port;
        private String baseUrl;
        private Map<String, String> metadata;
        private ServiceStatus status;
        private LocalDateTime lastHeartbeat;
        private LocalDateTime registeredAt;
        private long responseTime;
        private int failureCount;
        private double weight;

        // Constructors, getters, and setters
        public ServiceInstance() {
            this.metadata = new HashMap<>();
            this.status = ServiceStatus.UNKNOWN;
            this.registeredAt = LocalDateTime.now();
            this.lastHeartbeat = LocalDateTime.now();
            this.weight = 1.0;
            this.failureCount = 0;
        }

        public ServiceInstance(String id, String name, String version, String host, int port) {
            this();
            this.id = id;
            this.name = name;
            this.version = version;
            this.host = host;
            this.port = port;
            this.baseUrl = "http://" + host + ":" + port;
        }

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
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
        
        public ServiceStatus getStatus() { return status; }
        public void setStatus(ServiceStatus status) { this.status = status; }
        
        public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
        public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
        
        public LocalDateTime getRegisteredAt() { return registeredAt; }
        public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
        
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
        
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
    }

    public enum ServiceStatus {
        HEALTHY,
        UNHEALTHY,
        UNKNOWN,
        MAINTENANCE,
        STARTING,
        STOPPING
    }

    // Service registry storage
    private final ConcurrentHashMap<String, ServiceInstance> serviceRegistry = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Configuration
    private static final long HEARTBEAT_INTERVAL = 30000; // 30 seconds
    private static final long CLEANUP_INTERVAL = 60000; // 1 minute
    private static final long INSTANCE_TIMEOUT = 120000; // 2 minutes

    public ServiceRegistryManager() {
        startHeartbeatMonitoring();
        startCleanupTask();
    }

    /**
     * Register a service instance
     */
    public void registerService(ServiceInstance instance) {
        instance.setRegisteredAt(LocalDateTime.now());
        instance.setLastHeartbeat(LocalDateTime.now());
        instance.setStatus(ServiceStatus.HEALTHY);
        
        // Add default metadata
        instance.getMetadata().put("environment", environment);
        instance.getMetadata().put("region", region);
        instance.getMetadata().put("registered_at", instance.getRegisteredAt().toString());
        
        serviceRegistry.put(instance.getId(), instance);
        logger.info("📝 Registered service instance: {} ({}:{})", 
                   instance.getId(), instance.getHost(), instance.getPort());
    }

    /**
     * Unregister a service instance
     */
    public void unregisterService(String instanceId) {
        ServiceInstance removed = serviceRegistry.remove(instanceId);
        if (removed != null) {
            logger.info("🗑️ Unregistered service instance: {}", instanceId);
        }
    }

    /**
     * Update service heartbeat
     */
    public void updateHeartbeat(String instanceId) {
        ServiceInstance instance = serviceRegistry.get(instanceId);
        if (instance != null) {
            instance.setLastHeartbeat(LocalDateTime.now());
            if (instance.getStatus() != ServiceStatus.HEALTHY) {
                instance.setStatus(ServiceStatus.HEALTHY);
                instance.setFailureCount(0);
                logger.info("✅ Service instance {} is back online", instanceId);
            }
        }
    }

    /**
     * Update service health status
     */
    public void updateServiceHealth(String instanceId, ServiceStatus status, long responseTime) {
        ServiceInstance instance = serviceRegistry.get(instanceId);
        if (instance != null) {
            instance.setStatus(status);
            instance.setResponseTime(responseTime);
            instance.setLastHeartbeat(LocalDateTime.now());
            
            if (status == ServiceStatus.UNHEALTHY) {
                instance.setFailureCount(instance.getFailureCount() + 1);
                instance.setWeight(Math.max(instance.getWeight() * 0.8, 0.1)); // Reduce weight
            } else if (status == ServiceStatus.HEALTHY) {
                instance.setFailureCount(0);
                instance.setWeight(Math.min(instance.getWeight() * 1.1, 2.0)); // Increase weight
            }
        }
    }

    /**
     * Get all healthy service instances
     */
    public List<ServiceInstance> getHealthyInstances() {
        return serviceRegistry.values().stream()
                .filter(instance -> instance.getStatus() == ServiceStatus.HEALTHY)
                .sorted(Comparator.comparingLong(ServiceInstance::getResponseTime))
                .toList();
    }

    /**
     * Get service instances by version
     */
    public List<ServiceInstance> getInstancesByVersion(String version) {
        return serviceRegistry.values().stream()
                .filter(instance -> version.equals(instance.getVersion()))
                .filter(instance -> instance.getStatus() == ServiceStatus.HEALTHY)
                .toList();
    }

    /**
     * Get service instances by metadata
     */
    public List<ServiceInstance> getInstancesByMetadata(String key, String value) {
        return serviceRegistry.values().stream()
                .filter(instance -> value.equals(instance.getMetadata().get(key)))
                .filter(instance -> instance.getStatus() == ServiceStatus.HEALTHY)
                .toList();
    }

    /**
     * Get best service instance using load balancing
     */
    public ServiceInstance getBestInstance() {
        List<ServiceInstance> healthyInstances = getHealthyInstances();
        if (healthyInstances.isEmpty()) {
            return null;
        }

        // Weighted random selection
        double totalWeight = healthyInstances.stream().mapToDouble(ServiceInstance::getWeight).sum();
        if (totalWeight <= 0) {
            return healthyInstances.get(0); // Fallback to first instance
        }

        double randomValue = Math.random() * totalWeight;
        double currentWeight = 0;

        for (ServiceInstance instance : healthyInstances) {
            currentWeight += instance.getWeight();
            if (randomValue <= currentWeight) {
                return instance;
            }
        }

        return healthyInstances.get(0); // Fallback
    }

    /**
     * Get service registry statistics
     */
    public Map<String, Object> getRegistryStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<ServiceInstance> instances = new ArrayList<>(serviceRegistry.values());
        
        stats.put("total_instances", instances.size());
        stats.put("healthy_instances", instances.stream().mapToLong(i -> i.getStatus() == ServiceStatus.HEALTHY ? 1 : 0).sum());
        stats.put("unhealthy_instances", instances.stream().mapToLong(i -> i.getStatus() == ServiceStatus.UNHEALTHY ? 1 : 0).sum());
        
        // Version distribution
        Map<String, Long> versionStats = instances.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    ServiceInstance::getVersion,
                    java.util.stream.Collectors.counting()
                ));
        stats.put("version_distribution", versionStats);
        
        // Average response time
        double avgResponseTime = instances.stream()
                .filter(i -> i.getStatus() == ServiceStatus.HEALTHY)
                .mapToLong(ServiceInstance::getResponseTime)
                .average()
                .orElse(0.0);
        stats.put("average_response_time", avgResponseTime);
        
        stats.put("last_updated", LocalDateTime.now().toString());
        
        return stats;
    }

    /**
     * Start heartbeat monitoring
     */
    private void startHeartbeatMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitorHeartbeats();
            } catch (Exception e) {
                logger.error("Error during heartbeat monitoring", e);
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Monitor service heartbeats
     */
    private void monitorHeartbeats() {
        LocalDateTime now = LocalDateTime.now();
        
        serviceRegistry.values().forEach(instance -> {
            long timeSinceLastHeartbeat = java.time.Duration.between(instance.getLastHeartbeat(), now).toMillis();
            
            if (timeSinceLastHeartbeat > INSTANCE_TIMEOUT) {
                if (instance.getStatus() == ServiceStatus.HEALTHY) {
                    instance.setStatus(ServiceStatus.UNHEALTHY);
                    logger.warn("⚠️ Service instance {} marked as unhealthy (no heartbeat for {}ms)", 
                               instance.getId(), timeSinceLastHeartbeat);
                }
            }
        });
    }

    /**
     * Start cleanup task for removing stale instances
     */
    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupStaleInstances();
            } catch (Exception e) {
                logger.error("Error during cleanup", e);
            }
        }, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Remove stale service instances
     */
    private void cleanupStaleInstances() {
        LocalDateTime now = LocalDateTime.now();
        
        serviceRegistry.entrySet().removeIf(entry -> {
            ServiceInstance instance = entry.getValue();
            long timeSinceLastHeartbeat = java.time.Duration.between(instance.getLastHeartbeat(), now).toMillis();
            
            if (timeSinceLastHeartbeat > INSTANCE_TIMEOUT * 2) { // Remove after 4 minutes
                logger.info("🗑️ Removing stale service instance: {} (last heartbeat: {})", 
                           instance.getId(), instance.getLastHeartbeat());
                return true;
            }
            return false;
        });
    }

    /**
     * Shutdown the service registry
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
