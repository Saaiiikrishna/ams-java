package com.example.attendancesystem.subscriber.service;

import com.example.attendancesystem.config.ServiceDiscoveryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * mDNS Service Discovery implementation using JmDNS
 * Handles service registration, discovery, and monitoring
 */
@Service
public class MdnsServiceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(MdnsServiceDiscovery.class);

    @Autowired
    private ServiceDiscoveryConfig config;

    private JmDNS jmdns;
    private ServiceInfo serviceInfo;
    private final Map<String, ServiceInfo> discoveredServices = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private boolean isRunning = false;

    /**
     * Initialize and start the mDNS service discovery
     */
    public void start() {
        if (!config.isEnabled()) {
            logger.info("mDNS Service Discovery is disabled");
            return;
        }

        try {
            logger.info("Starting mDNS Service Discovery...");
            
            // Get the appropriate network interface
            InetAddress address = getNetworkAddress();
            logger.info("Using network address: {}", address.getHostAddress());

            // Create JmDNS instance
            jmdns = JmDNS.create(address);
            
            // Register our service if auto-register is enabled
            if (config.isAutoRegister()) {
                registerService();
            }

            // Start service discovery
            startServiceDiscovery();

            // Schedule periodic health checks
            scheduleHealthChecks();

            isRunning = true;
            logger.info("mDNS Service Discovery started successfully");

        } catch (Exception e) {
            logger.error("Failed to start mDNS Service Discovery", e);
            throw new RuntimeException("mDNS Service Discovery startup failed", e);
        }
    }

    /**
     * Stop the mDNS service discovery
     */
    public void stop() {
        if (!isRunning) {
            return;
        }

        try {
            logger.info("Stopping mDNS Service Discovery...");

            // Unregister our service
            if (serviceInfo != null) {
                jmdns.unregisterService(serviceInfo);
                logger.info("Service unregistered: {}", serviceInfo.getName());
            }

            // Close JmDNS
            if (jmdns != null) {
                jmdns.close();
            }

            // Shutdown scheduler
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            isRunning = false;
            logger.info("mDNS Service Discovery stopped");

        } catch (Exception e) {
            logger.error("Error stopping mDNS Service Discovery", e);
        }
    }

    /**
     * Register this service instance with mDNS
     */
    private void registerService() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("version", config.getVersion());
        properties.put("environment", config.getEnvironment());
        properties.put("region", config.getRegion());
        properties.put("grpc-port", String.valueOf(config.getPort()));
        properties.put("started-at", String.valueOf(System.currentTimeMillis()));

        serviceInfo = ServiceInfo.create(
                config.getServiceType(),
                config.getServiceInstanceName(),
                config.getPort(),
                config.getWeight(),
                config.getPriority(),
                properties
        );

        jmdns.registerService(serviceInfo);
        logger.info("Service registered: {} on port {}", serviceInfo.getName(), config.getPort());
    }

    /**
     * Start discovering other services
     */
    private void startServiceDiscovery() {
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent event) {
                logger.debug("Service added: {}", event.getInfo().getName());
                // Request service info to get full details
                jmdns.requestServiceInfo(event.getType(), event.getName());
            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
                String serviceName = event.getInfo().getName();
                logger.info("Service removed: {}", serviceName);
                discoveredServices.remove(serviceName);
            }

            @Override
            public void serviceResolved(ServiceEvent event) {
                ServiceInfo info = event.getInfo();
                if (info != null && !info.getName().equals(serviceInfo.getName())) {
                    logger.info("Service discovered: {} at {}:{}", 
                            info.getName(), 
                            info.getHostAddresses()[0], 
                            info.getPort());
                    discoveredServices.put(info.getName(), info);
                }
            }
        };

        // Listen for our service type
        jmdns.addServiceListener(config.getServiceType(), listener);
        logger.info("Started listening for services of type: {}", config.getServiceType());
    }

    /**
     * Schedule periodic health checks and service refresh
     */
    private void scheduleHealthChecks() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Refresh service discovery
                logger.debug("Performing periodic service discovery refresh");
                
                // Check if our service is still registered
                if (serviceInfo != null) {
                    ServiceInfo[] services = jmdns.list(config.getServiceType());
                    boolean found = Arrays.stream(services)
                            .anyMatch(s -> s.getName().equals(serviceInfo.getName()));
                    
                    if (!found) {
                        logger.warn("Our service is not found in mDNS registry, re-registering...");
                        registerService();
                    }
                }

            } catch (Exception e) {
                logger.error("Error during health check", e);
            }
        }, config.getDiscoveryInterval(), config.getDiscoveryInterval(), TimeUnit.SECONDS);
    }

    /**
     * Get the appropriate network address for mDNS
     */
    private InetAddress getNetworkAddress() throws SocketException, java.net.UnknownHostException {
        if (!"auto".equals(config.getNetworkInterface())) {
            // Use specific network interface
            NetworkInterface ni = NetworkInterface.getByName(config.getNetworkInterface());
            if (ni != null) {
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (isValidAddress(addr)) {
                        return addr;
                    }
                }
            }
        }

        // Auto-detect best network interface
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual()) {
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (isValidAddress(addr)) {
                        return addr;
                    }
                }
            }
        }

        // NO FALLBACK TO LOCALHOST - throw exception if no valid interface found
        throw new RuntimeException("No valid network interface found for mDNS. " +
                "Ensure the system has an active non-loopback network interface.");
    }

    /**
     * Check if an IP address is valid for our configuration
     */
    private boolean isValidAddress(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
            return false;
        }

        if (addr.getAddress().length == 4 && config.isUseIpv4()) {
            return true;
        }

        if (addr.getAddress().length == 16 && config.isUseIpv6()) {
            return true;
        }

        return false;
    }

    /**
     * Get all discovered services
     */
    public Map<String, ServiceInfo> getDiscoveredServices() {
        return new HashMap<>(discoveredServices);
    }

    /**
     * Find a specific service by name
     */
    public Optional<ServiceInfo> findService(String serviceName) {
        return Optional.ofNullable(discoveredServices.get(serviceName));
    }

    /**
     * Get services by type
     */
    public List<ServiceInfo> getServicesByType(String serviceType) {
        if (jmdns == null) {
            return Collections.emptyList();
        }

        try {
            ServiceInfo[] services = jmdns.list(serviceType);
            return Arrays.asList(services);
        } catch (Exception e) {
            logger.error("Error listing services of type: {}", serviceType, e);
            return Collections.emptyList();
        }
    }

    /**
     * Check if the service discovery is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get our registered service info
     */
    public ServiceInfo getOurServiceInfo() {
        return serviceInfo;
    }
}
