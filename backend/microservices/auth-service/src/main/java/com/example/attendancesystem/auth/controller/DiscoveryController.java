package com.example.attendancesystem.auth.controller;

import com.example.attendancesystem.auth.discovery.NetworkDiscoveryService;
import com.example.attendancesystem.auth.discovery.ServiceDiscoveryResponse;
import com.example.attendancesystem.auth.discovery.ServiceInfo;
import com.example.attendancesystem.auth.dto.SuccessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Discovery Controller for mobile clients and service discovery
 * Provides endpoints for network discovery and service information
 */
@RestController
@RequestMapping("/api/discovery")
@CrossOrigin(origins = "*") // Allow all origins for discovery
public class DiscoveryController {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryController.class);

    @Autowired
    private NetworkDiscoveryService networkDiscoveryService;

    /**
     * Main discovery endpoint for mobile clients
     * Returns comprehensive service information for connection
     */
    @GetMapping("/services")
    public ResponseEntity<ServiceDiscoveryResponse> getServiceDiscovery() {
        try {
            logger.debug("Service discovery request received");
            ServiceDiscoveryResponse response = networkDiscoveryService.getDiscoveryInfo();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in service discovery", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lightweight discovery endpoint for quick checks
     * Returns basic service information
     */
    @GetMapping("/ping")
    public ResponseEntity<SuccessResponse> ping() {
        try {
            ServiceDiscoveryResponse discoveryInfo = networkDiscoveryService.getDiscoveryInfo();
            SuccessResponse response = new SuccessResponse(
                "Service discovery active",
                Map.of(
                    "serviceName", discoveryInfo.getCurrentService().getServiceName(),
                    "httpEndpoint", discoveryInfo.getPrimaryEndpoint(),
                    "grpcEndpoint", discoveryInfo.getPrimaryGrpcEndpoint(),
                    "timestamp", System.currentTimeMillis()
                )
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in discovery ping", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all discovered services on the network
     * Useful for debugging and network monitoring
     */
    @GetMapping("/network")
    public ResponseEntity<SuccessResponse> getNetworkServices() {
        try {
            Map<String, ServiceInfo> discoveredServices = networkDiscoveryService.getDiscoveredServices();
            SuccessResponse response = new SuccessResponse(
                "Network services retrieved",
                Map.of(
                    "discoveredServices", discoveredServices,
                    "serviceCount", discoveredServices.size(),
                    "timestamp", System.currentTimeMillis()
                )
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting network services", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint specifically for discovery
     */
    @GetMapping("/health")
    public ResponseEntity<SuccessResponse> discoveryHealth() {
        try {
            ServiceDiscoveryResponse discoveryInfo = networkDiscoveryService.getDiscoveryInfo();
            boolean isHealthy = discoveryInfo.getCurrentService() != null && 
                              discoveryInfo.getCurrentService().isActive();
            
            SuccessResponse response = new SuccessResponse(
                isHealthy ? "Discovery service healthy" : "Discovery service degraded",
                Map.of(
                    "healthy", isHealthy,
                    "activeNetworks", discoveryInfo.getActiveNetworks().size(),
                    "discoveredServices", discoveryInfo.getDiscoveredServices().size(),
                    "uptime", System.currentTimeMillis() - discoveryInfo.getCurrentService().getTimestamp()
                )
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in discovery health check", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Mobile-specific discovery endpoint
     * Optimized response for mobile applications
     */
    @GetMapping("/mobile")
    public ResponseEntity<MobileDiscoveryResponse> getMobileDiscovery() {
        try {
            ServiceDiscoveryResponse discoveryInfo = networkDiscoveryService.getDiscoveryInfo();
            MobileDiscoveryResponse response = new MobileDiscoveryResponse(discoveryInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in mobile discovery", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Optimized discovery response for mobile clients
     */
    public static class MobileDiscoveryResponse {
        private String serverUrl;
        private String grpcUrl;
        private boolean available;
        private long timestamp;
        private ConnectionConfig connectionConfig;

        public MobileDiscoveryResponse(ServiceDiscoveryResponse discoveryInfo) {
            if (discoveryInfo.getCurrentService() != null) {
                this.serverUrl = discoveryInfo.getPrimaryEndpoint();
                this.grpcUrl = discoveryInfo.getPrimaryGrpcEndpoint();
                this.available = true;
            } else {
                this.available = false;
            }
            this.timestamp = System.currentTimeMillis();
            this.connectionConfig = new ConnectionConfig();
        }

        // Getters and Setters
        public String getServerUrl() { return serverUrl; }
        public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

        public String getGrpcUrl() { return grpcUrl; }
        public void setGrpcUrl(String grpcUrl) { this.grpcUrl = grpcUrl; }

        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public ConnectionConfig getConnectionConfig() { return connectionConfig; }
        public void setConnectionConfig(ConnectionConfig connectionConfig) { this.connectionConfig = connectionConfig; }

        public static class ConnectionConfig {
            private int timeoutSeconds = 10;
            private int retryAttempts = 3;
            private int heartbeatInterval = 30;
            private boolean autoReconnect = true;

            // Getters and Setters
            public int getTimeoutSeconds() { return timeoutSeconds; }
            public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

            public int getRetryAttempts() { return retryAttempts; }
            public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }

            public int getHeartbeatInterval() { return heartbeatInterval; }
            public void setHeartbeatInterval(int heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }

            public boolean isAutoReconnect() { return autoReconnect; }
            public void setAutoReconnect(boolean autoReconnect) { this.autoReconnect = autoReconnect; }
        }
    }
}
