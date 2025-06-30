package com.example.attendancesystem.auth.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Network Discovery Service for seamless client-server communication
 * Supports dynamic network changes and automatic reconnection
 */
@Service
public class NetworkDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkDiscoveryService.class);
    
    @Value("${server.port:8081}")
    private int httpPort;
    
    @Value("${grpc.server.port:9091}")
    private int grpcPort;
    
    @Value("${discovery.broadcast.port:8888}")
    private int discoveryPort;
    
    @Value("${discovery.service.name:AMS-AUTH-SERVICE}")
    private String serviceName;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final Set<String> activeNetworkInterfaces = ConcurrentHashMap.newKeySet();
    private final Map<String, ServiceInfo> discoveredServices = new ConcurrentHashMap<>();
    
    private DatagramSocket broadcastSocket;
    private volatile boolean isRunning = false;
    
    @PostConstruct
    public void startDiscovery() {
        logger.info("Starting Network Discovery Service...");
        try {
            initializeBroadcastSocket();
            startNetworkMonitoring();
            startServiceBroadcast();
            startServiceDiscovery();
            isRunning = true;
            logger.info("Network Discovery Service started successfully");
        } catch (Exception e) {
            logger.error("Failed to start Network Discovery Service", e);
        }
    }
    
    @PreDestroy
    public void stopDiscovery() {
        logger.info("Stopping Network Discovery Service...");
        isRunning = false;
        scheduler.shutdown();
        if (broadcastSocket != null && !broadcastSocket.isClosed()) {
            broadcastSocket.close();
        }
        logger.info("Network Discovery Service stopped");
    }
    
    private void initializeBroadcastSocket() throws SocketException {
        broadcastSocket = new DatagramSocket();
        broadcastSocket.setBroadcast(true);
        broadcastSocket.setSoTimeout(5000); // 5 second timeout
    }
    
    /**
     * Monitor network interfaces for changes
     */
    private void startNetworkMonitoring() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                Set<String> currentInterfaces = getCurrentNetworkInterfaces();
                
                // Detect new interfaces
                for (String iface : currentInterfaces) {
                    if (!activeNetworkInterfaces.contains(iface)) {
                        logger.info("New network interface detected: {}", iface);
                        activeNetworkInterfaces.add(iface);
                        onNetworkChange();
                    }
                }
                
                // Detect removed interfaces
                Set<String> removedInterfaces = new HashSet<>(activeNetworkInterfaces);
                removedInterfaces.removeAll(currentInterfaces);
                for (String iface : removedInterfaces) {
                    logger.info("Network interface removed: {}", iface);
                    activeNetworkInterfaces.remove(iface);
                    onNetworkChange();
                }
                
            } catch (Exception e) {
                logger.warn("Error monitoring network interfaces", e);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
    
    /**
     * Broadcast service information to local network
     */
    private void startServiceBroadcast() {
        scheduler.scheduleWithFixedDelay(() -> {
            if (!isRunning) return;
            
            try {
                ServiceInfo serviceInfo = createServiceInfo();
                String message = serviceInfo.toJson();
                byte[] data = message.getBytes();
                
                // Broadcast to all network interfaces
                for (String networkInterface : activeNetworkInterfaces) {
                    try {
                        InetAddress broadcastAddr = getBroadcastAddress(networkInterface);
                        if (broadcastAddr != null) {
                            DatagramPacket packet = new DatagramPacket(
                                data, data.length, broadcastAddr, discoveryPort);
                            broadcastSocket.send(packet);
                            logger.debug("Broadcasted service info to {}", broadcastAddr);
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to broadcast to interface {}: {}", networkInterface, e.getMessage());
                    }
                }
                
            } catch (Exception e) {
                logger.warn("Error broadcasting service information", e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Listen for service discoveries from other services
     */
    private void startServiceDiscovery() {
        scheduler.submit(() -> {
            try (DatagramSocket listenSocket = new DatagramSocket(discoveryPort)) {
                listenSocket.setSoTimeout(1000);
                byte[] buffer = new byte[1024];
                
                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        listenSocket.receive(packet);
                        
                        String message = new String(packet.getData(), 0, packet.getLength());
                        ServiceInfo serviceInfo = ServiceInfo.fromJson(message);
                        
                        if (serviceInfo != null && !serviceInfo.getServiceId().equals(getServiceId())) {
                            discoveredServices.put(serviceInfo.getServiceId(), serviceInfo);
                            logger.debug("Discovered service: {}", serviceInfo);
                        }
                        
                    } catch (SocketTimeoutException e) {
                        // Normal timeout, continue listening
                    } catch (Exception e) {
                        logger.debug("Error in service discovery: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to start service discovery listener", e);
            }
        });
    }
    
    private Set<String> getCurrentNetworkInterfaces() {
        Set<String> interfaces = new HashSet<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    interfaces.add(networkInterface.getName());
                }
            }
        } catch (Exception e) {
            logger.warn("Error getting network interfaces", e);
        }
        return interfaces;
    }
    
    private InetAddress getBroadcastAddress(String interfaceName) {
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
            if (networkInterface != null) {
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        return broadcast;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting broadcast address for {}: {}", interfaceName, e.getMessage());
        }
        return null;
    }
    
    private ServiceInfo createServiceInfo() {
        return new ServiceInfo(
            getServiceId(),
            serviceName,
            getLocalIPAddress(),
            httpPort,
            grpcPort,
            System.currentTimeMillis()
        );
    }
    
    private String getServiceId() {
        return serviceName + "-" + getLocalIPAddress() + "-" + httpPort;
    }
    
    private String getLocalIPAddress() {
        try {
            // Try to get the most appropriate local IP address
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error getting local IP address", e);
        }
        return "localhost";
    }
    
    private void onNetworkChange() {
        logger.info("Network change detected, updating service discovery...");
        // Clear old discovered services as they might be invalid
        discoveredServices.clear();
        // Force immediate broadcast
        scheduler.submit(this::startServiceBroadcast);
    }
    
    /**
     * Get all discovered services
     */
    public Map<String, ServiceInfo> getDiscoveredServices() {
        // Remove stale services (older than 2 minutes)
        long cutoffTime = System.currentTimeMillis() - 120000;
        discoveredServices.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp() < cutoffTime);
        
        return new HashMap<>(discoveredServices);
    }
    
    /**
     * Get service discovery information for mobile clients
     */
    public ServiceDiscoveryResponse getDiscoveryInfo() {
        return new ServiceDiscoveryResponse(
            createServiceInfo(),
            new ArrayList<>(discoveredServices.values()),
            activeNetworkInterfaces
        );
    }
}
