package com.example.attendancesystem.auth.discovery;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service information for network discovery
 */
public class ServiceInfo {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @JsonProperty("serviceId")
    private String serviceId;
    
    @JsonProperty("serviceName")
    private String serviceName;
    
    @JsonProperty("ipAddress")
    private String ipAddress;
    
    @JsonProperty("httpPort")
    private int httpPort;
    
    @JsonProperty("grpcPort")
    private int grpcPort;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("version")
    private String version = "1.0.0";
    
    @JsonProperty("status")
    private String status = "ACTIVE";

    public ServiceInfo() {}

    public ServiceInfo(String serviceId, String serviceName, String ipAddress, 
                      int httpPort, int grpcPort, long timestamp) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.ipAddress = ipAddress;
        this.httpPort = httpPort;
        this.grpcPort = grpcPort;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public void setGrpcPort(int grpcPort) {
        this.grpcPort = grpcPort;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Convert to JSON string for network transmission
     */
    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * Create ServiceInfo from JSON string
     */
    public static ServiceInfo fromJson(String json) {
        try {
            return objectMapper.readValue(json, ServiceInfo.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get HTTP endpoint URL
     */
    public String getHttpUrl() {
        return "http://" + ipAddress + ":" + httpPort;
    }

    /**
     * Get gRPC endpoint
     */
    public String getGrpcEndpoint() {
        return ipAddress + ":" + grpcPort;
    }

    /**
     * Check if service is still active (within last 2 minutes)
     */
    public boolean isActive() {
        return System.currentTimeMillis() - timestamp < 120000; // 2 minutes
    }

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "serviceId='" + serviceId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", httpPort=" + httpPort +
                ", grpcPort=" + grpcPort +
                ", timestamp=" + timestamp +
                ", version='" + version + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInfo that = (ServiceInfo) o;
        return serviceId != null ? serviceId.equals(that.serviceId) : that.serviceId == null;
    }

    @Override
    public int hashCode() {
        return serviceId != null ? serviceId.hashCode() : 0;
    }
}
