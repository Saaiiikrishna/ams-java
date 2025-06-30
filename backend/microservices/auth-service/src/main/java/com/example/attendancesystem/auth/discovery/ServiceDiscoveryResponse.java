package com.example.attendancesystem.auth.discovery;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;

/**
 * Response DTO for service discovery information
 * Used by mobile clients to discover and connect to services
 */
public class ServiceDiscoveryResponse {
    
    @JsonProperty("currentService")
    private ServiceInfo currentService;
    
    @JsonProperty("discoveredServices")
    private List<ServiceInfo> discoveredServices;
    
    @JsonProperty("activeNetworks")
    private Set<String> activeNetworks;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("discoveryVersion")
    private String discoveryVersion = "2.0";

    public ServiceDiscoveryResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ServiceDiscoveryResponse(ServiceInfo currentService, 
                                  List<ServiceInfo> discoveredServices, 
                                  Set<String> activeNetworks) {
        this();
        this.currentService = currentService;
        this.discoveredServices = discoveredServices;
        this.activeNetworks = activeNetworks;
    }

    // Getters and Setters
    public ServiceInfo getCurrentService() {
        return currentService;
    }

    public void setCurrentService(ServiceInfo currentService) {
        this.currentService = currentService;
    }

    public List<ServiceInfo> getDiscoveredServices() {
        return discoveredServices;
    }

    public void setDiscoveredServices(List<ServiceInfo> discoveredServices) {
        this.discoveredServices = discoveredServices;
    }

    public Set<String> getActiveNetworks() {
        return activeNetworks;
    }

    public void setActiveNetworks(Set<String> activeNetworks) {
        this.activeNetworks = activeNetworks;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDiscoveryVersion() {
        return discoveryVersion;
    }

    public void setDiscoveryVersion(String discoveryVersion) {
        this.discoveryVersion = discoveryVersion;
    }

    /**
     * Get the primary service endpoint for mobile clients
     */
    @JsonProperty("primaryEndpoint")
    public String getPrimaryEndpoint() {
        if (currentService != null) {
            return currentService.getHttpUrl();
        }
        return null;
    }

    /**
     * Get the primary gRPC endpoint for mobile clients
     */
    @JsonProperty("primaryGrpcEndpoint")
    public String getPrimaryGrpcEndpoint() {
        if (currentService != null) {
            return currentService.getGrpcEndpoint();
        }
        return null;
    }

    /**
     * Get connection instructions for mobile clients
     */
    @JsonProperty("connectionInstructions")
    public ConnectionInstructions getConnectionInstructions() {
        return new ConnectionInstructions();
    }

    /**
     * Inner class for connection instructions
     */
    public static class ConnectionInstructions {
        @JsonProperty("httpEndpoint")
        public String httpEndpoint;
        
        @JsonProperty("grpcEndpoint") 
        public String grpcEndpoint;
        
        @JsonProperty("authRequired")
        public boolean authRequired = true;
        
        @JsonProperty("supportedAuthMethods")
        public String[] supportedAuthMethods = {"JWT", "BEARER"};
        
        @JsonProperty("healthCheckPath")
        public String healthCheckPath = "/auth/actuator/health";
        
        @JsonProperty("discoveryRefreshInterval")
        public int discoveryRefreshInterval = 30; // seconds
        
        @JsonProperty("connectionTimeout")
        public int connectionTimeout = 10; // seconds
        
        @JsonProperty("retryAttempts")
        public int retryAttempts = 3;
    }
}
