package com.example.attendancesystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for mDNS service discovery
 */
@Configuration
@ConfigurationProperties(prefix = "service.discovery")
public class ServiceDiscoveryConfig {

    private boolean enabled = true;
    private String serviceName = "attendance-system";
    private String serviceType = "_grpc._tcp.local.";
    private int port = 9090;
    private int weight = 0;
    private int priority = 0;
    private String domain = "local.";
    private long ttl = 120; // Time to live in seconds
    private boolean autoRegister = true;
    private int discoveryInterval = 30; // Discovery interval in seconds

    // Service metadata
    private String version = "1.0.0";
    private String environment = "development";
    private String region = "local";

    // Network configuration
    private String networkInterface = "auto"; // auto-detect or specific interface name
    private boolean useIpv4 = true;
    private boolean useIpv6 = false;

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public boolean isAutoRegister() {
        return autoRegister;
    }

    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }

    public int getDiscoveryInterval() {
        return discoveryInterval;
    }

    public void setDiscoveryInterval(int discoveryInterval) {
        this.discoveryInterval = discoveryInterval;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getNetworkInterface() {
        return networkInterface;
    }

    public void setNetworkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
    }

    public boolean isUseIpv4() {
        return useIpv4;
    }

    public void setUseIpv4(boolean useIpv4) {
        this.useIpv4 = useIpv4;
    }

    public boolean isUseIpv6() {
        return useIpv6;
    }

    public void setUseIpv6(boolean useIpv6) {
        this.useIpv6 = useIpv6;
    }

    /**
     * Get the full service name for mDNS registration
     */
    public String getFullServiceName() {
        return serviceName + "." + serviceType;
    }

    /**
     * Get service instance name with unique identifier
     */
    public String getServiceInstanceName() {
        return serviceName + "-" + System.currentTimeMillis();
    }
}
