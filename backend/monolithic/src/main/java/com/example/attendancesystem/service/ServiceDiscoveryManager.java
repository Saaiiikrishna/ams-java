package com.example.attendancesystem.subscriber.service;

import com.example.attendancesystem.config.ServiceDiscoveryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.jmdns.ServiceInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service Discovery Manager that handles the lifecycle and provides
 * a high-level API for service discovery operations
 */
@Service
public class ServiceDiscoveryManager {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryManager.class);

    @Autowired
    private MdnsServiceDiscovery mdnsServiceDiscovery;

    @Autowired
    private ServiceDiscoveryConfig config;

    private final Map<String, String> serviceEndpoints = new ConcurrentHashMap<>();

    /**
     * Start service discovery when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (config.isEnabled()) {
            logger.info("Application ready, starting service discovery...");
            try {
                mdnsServiceDiscovery.start();
                logger.info("Service discovery started successfully");
            } catch (Exception e) {
                logger.error("Failed to start service discovery", e);
            }
        }
    }

    /**
     * Stop service discovery when application context is closing
     */
    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown() {
        if (config.isEnabled()) {
            logger.info("Application shutting down, stopping service discovery...");
            try {
                mdnsServiceDiscovery.stop();
                logger.info("Service discovery stopped successfully");
            } catch (Exception e) {
                logger.error("Error stopping service discovery", e);
            }
        }
    }

    /**
     * Discover services of a specific type
     */
    public List<ServiceInfo> discoverServices(String serviceType) {
        if (!config.isEnabled()) {
            logger.warn("Service discovery is disabled");
            return List.of();
        }

        try {
            return mdnsServiceDiscovery.getServicesByType(serviceType);
        } catch (Exception e) {
            logger.error("Error discovering services of type: {}", serviceType, e);
            return List.of();
        }
    }

    /**
     * Find a specific service by name
     */
    public Optional<ServiceInfo> findService(String serviceName) {
        if (!config.isEnabled()) {
            logger.warn("Service discovery is disabled");
            return Optional.empty();
        }

        return mdnsServiceDiscovery.findService(serviceName);
    }

    /**
     * Get the endpoint URL for a service
     */
    public Optional<String> getServiceEndpoint(String serviceName) {
        // Check cache first
        String cachedEndpoint = serviceEndpoints.get(serviceName);
        if (cachedEndpoint != null) {
            return Optional.of(cachedEndpoint);
        }

        // Discover service
        Optional<ServiceInfo> serviceOpt = findService(serviceName);
        if (serviceOpt.isPresent()) {
            ServiceInfo service = serviceOpt.get();
            String endpoint = buildEndpoint(service);
            serviceEndpoints.put(serviceName, endpoint);
            return Optional.of(endpoint);
        }

        return Optional.empty();
    }

    /**
     * Get gRPC endpoint for a service
     */
    public Optional<String> getGrpcServiceEndpoint(String serviceName) {
        Optional<ServiceInfo> serviceOpt = findService(serviceName);
        if (serviceOpt.isPresent()) {
            ServiceInfo service = serviceOpt.get();
            if (service.getHostAddresses().length > 0) {
                String grpcPort = service.getPropertyString("grpc-port");
                int port = grpcPort != null ? Integer.parseInt(grpcPort) : service.getPort();
                return Optional.of(service.getHostAddresses()[0] + ":" + port);
            }
        }
        return Optional.empty();
    }

    /**
     * Get all discovered services
     */
    public Map<String, ServiceInfo> getAllDiscoveredServices() {
        if (!config.isEnabled()) {
            return Map.of();
        }

        return mdnsServiceDiscovery.getDiscoveredServices();
    }

    /**
     * Get our own service information
     */
    public Optional<ServiceInfo> getOurServiceInfo() {
        if (!config.isEnabled()) {
            return Optional.empty();
        }

        return Optional.ofNullable(mdnsServiceDiscovery.getOurServiceInfo());
    }

    /**
     * Check if service discovery is running
     */
    public boolean isRunning() {
        return config.isEnabled() && mdnsServiceDiscovery.isRunning();
    }

    /**
     * Refresh service discovery cache
     */
    public void refreshCache() {
        serviceEndpoints.clear();
        logger.info("Service discovery cache refreshed");
    }

    /**
     * Get service health status
     */
    public Map<String, Object> getServiceHealth() {
        Map<String, Object> health = new ConcurrentHashMap<>();
        health.put("enabled", config.isEnabled());
        health.put("running", isRunning());
        health.put("discoveredServices", getAllDiscoveredServices().size());
        health.put("cachedEndpoints", serviceEndpoints.size());
        
        if (config.isEnabled()) {
            Optional<ServiceInfo> ourService = getOurServiceInfo();
            health.put("registered", ourService.isPresent());
            if (ourService.isPresent()) {
                health.put("serviceName", ourService.get().getName());
                health.put("servicePort", ourService.get().getPort());
            }
        }
        
        return health;
    }

    /**
     * Build endpoint URL from service info
     */
    private String buildEndpoint(ServiceInfo service) {
        if (service.getHostAddresses().length > 0) {
            String protocol = service.getPropertyString("protocol");
            if (protocol == null) {
                protocol = "http"; // default protocol
            }
            return protocol + "://" + service.getHostAddresses()[0] + ":" + service.getPort();
        }
        return null;
    }

    /**
     * Find services by environment
     */
    public List<ServiceInfo> findServicesByEnvironment(String environment) {
        return getAllDiscoveredServices().values().stream()
                .filter(service -> environment.equals(service.getPropertyString("environment")))
                .toList();
    }

    /**
     * Find services by version
     */
    public List<ServiceInfo> findServicesByVersion(String version) {
        return getAllDiscoveredServices().values().stream()
                .filter(service -> version.equals(service.getPropertyString("version")))
                .toList();
    }

    /**
     * Get service statistics
     */
    public Map<String, Object> getServiceStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        Map<String, ServiceInfo> services = getAllDiscoveredServices();
        
        stats.put("totalServices", services.size());
        
        // Group by environment
        Map<String, Long> byEnvironment = services.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        service -> service.getPropertyString("environment") != null ? 
                                service.getPropertyString("environment") : "unknown",
                        java.util.stream.Collectors.counting()
                ));
        stats.put("byEnvironment", byEnvironment);
        
        // Group by version
        Map<String, Long> byVersion = services.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        service -> service.getPropertyString("version") != null ? 
                                service.getPropertyString("version") : "unknown",
                        java.util.stream.Collectors.counting()
                ));
        stats.put("byVersion", byVersion);
        
        return stats;
    }
}
