package com.example.attendancesystem.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jmdns.ServiceInfo;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating gRPC clients with service discovery integration
 * Manages connection pooling and automatic service endpoint resolution
 */
@Service
public class GrpcClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(GrpcClientFactory.class);

    @Autowired
    private ServiceDiscoveryManager serviceDiscoveryManager;

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    /**
     * Create a gRPC stub for a service using service discovery
     */
    public <T extends AbstractStub<T>> Optional<T> createStub(String serviceName, StubFactory<T> stubFactory) {
        try {
            ManagedChannel channel = getOrCreateChannel(serviceName);
            if (channel != null) {
                T stub = stubFactory.create(channel);
                logger.debug("Created gRPC stub for service: {}", serviceName);
                return Optional.of(stub);
            }
        } catch (Exception e) {
            logger.error("Failed to create gRPC stub for service: {}", serviceName, e);
        }
        return Optional.empty();
    }

    /**
     * Create a gRPC stub with explicit host and port
     */
    public <T extends AbstractStub<T>> T createStub(String host, int port, StubFactory<T> stubFactory) {
        String channelKey = host + ":" + port;
        ManagedChannel channel = channels.computeIfAbsent(channelKey, key -> {
            logger.info("Creating gRPC channel to {}:{}", host, port);
            return ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(5, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(true)
                    .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
                    .build();
        });

        return stubFactory.create(channel);
    }

    /**
     * Get or create a managed channel for a service
     */
    private ManagedChannel getOrCreateChannel(String serviceName) {
        // Check if we already have a channel for this service
        ManagedChannel existingChannel = channels.get(serviceName);
        if (existingChannel != null && !existingChannel.isShutdown()) {
            return existingChannel;
        }

        // Discover service endpoint
        Optional<String> endpointOpt = serviceDiscoveryManager.getGrpcServiceEndpoint(serviceName);
        if (endpointOpt.isEmpty()) {
            logger.warn("Service not found: {}", serviceName);
            return null;
        }

        String endpoint = endpointOpt.get();
        String[] parts = endpoint.split(":");
        if (parts.length != 2) {
            logger.error("Invalid endpoint format: {}", endpoint);
            return null;
        }

        String host = parts[0];
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            logger.error("Invalid port in endpoint: {}", endpoint);
            return null;
        }

        // Create new channel
        logger.info("Creating gRPC channel for service {} at {}:{}", serviceName, host, port);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
                .build();

        channels.put(serviceName, channel);
        return channel;
    }

    /**
     * Get service information for a discovered service
     */
    public Optional<ServiceInfo> getServiceInfo(String serviceName) {
        return serviceDiscoveryManager.findService(serviceName);
    }

    /**
     * Check if a service is available
     */
    public boolean isServiceAvailable(String serviceName) {
        return serviceDiscoveryManager.findService(serviceName).isPresent();
    }

    /**
     * Get all available services
     */
    public Map<String, ServiceInfo> getAvailableServices() {
        return serviceDiscoveryManager.getAllDiscoveredServices();
    }

    /**
     * Refresh service discovery and close stale connections
     */
    public void refreshConnections() {
        logger.info("Refreshing gRPC connections...");
        
        // Close all existing channels
        channels.values().forEach(channel -> {
            if (!channel.isShutdown()) {
                channel.shutdown();
                try {
                    if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                        channel.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    channel.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        channels.clear();
        serviceDiscoveryManager.refreshCache();
        
        logger.info("gRPC connections refreshed");
    }

    /**
     * Shutdown all channels
     */
    public void shutdown() {
        logger.info("Shutting down gRPC client factory...");
        
        channels.values().forEach(channel -> {
            if (!channel.isShutdown()) {
                channel.shutdown();
                try {
                    if (!channel.awaitTermination(10, TimeUnit.SECONDS)) {
                        channel.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    channel.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        channels.clear();
        logger.info("gRPC client factory shutdown complete");
    }

    /**
     * Get connection statistics
     */
    public Map<String, Object> getConnectionStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalChannels", channels.size());
        
        long activeChannels = channels.values().stream()
                .mapToLong(channel -> channel.isShutdown() ? 0 : 1)
                .sum();
        stats.put("activeChannels", activeChannels);
        
        Map<String, String> channelStates = new ConcurrentHashMap<>();
        channels.forEach((serviceName, channel) -> {
            channelStates.put(serviceName, channel.getState(false).toString());
        });
        stats.put("channelStates", channelStates);
        
        return stats;
    }

    /**
     * Functional interface for creating gRPC stubs
     */
    @FunctionalInterface
    public interface StubFactory<T extends AbstractStub<T>> {
        T create(ManagedChannel channel);
    }
}
