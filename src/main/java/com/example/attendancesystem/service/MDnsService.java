package com.example.attendancesystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for mDNS (Multicast DNS) service discovery
 * Advertises the backend API service on the local network with fixed hostname
 * Supports dynamic network change detection and re-registration
 */
@Service
public class MDnsService {

    private static final Logger logger = LoggerFactory.getLogger(MDnsService.class);

    // Service types for different purposes
    private static final String API_SERVICE_TYPE = "_attendanceapi._tcp.local.";
    private static final String HTTP_SERVICE_TYPE = "_http._tcp.local.";
    private static final String FIXED_HOSTNAME = "restaurant.local";
    private static final String SERVICE_NAME = "RestaurantSystem";

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${app.mdns.enabled:true}")
    private boolean mdnsEnabled;

    @Value("${app.mdns.hostname:restaurant.local}")
    private String mdnsHostname;

    private JmDNS jmdns;
    private ServiceInfo apiServiceInfo;
    private ServiceInfo httpServiceInfo;
    private String currentIPAddress;
    private volatile boolean isServiceRunning = false;
    
    /**
     * Start mDNS service advertisement with proper hostname registration
     */
    public void startService() {
        if (!mdnsEnabled) {
            logger.info("mDNS service is disabled");
            return;
        }

        try {
            logger.info("🚀 Starting mDNS service advertisement...");
            logger.info("   Target hostname: {}", mdnsHostname);

            // Get best available IP address for the current network
            InetAddress localAddress = getBestLocalAddress();
            currentIPAddress = localAddress.getHostAddress();
            logger.info("   Selected IP address: {}", currentIPAddress);

            // Stop existing service if running
            if (isServiceRunning) {
                logger.info("   Stopping existing mDNS service...");
                stopService();
            }

            // Create JmDNS instance
            jmdns = JmDNS.create(localAddress);

            // Wait a moment for JmDNS to initialize
            Thread.sleep(1000);

            // 1. Register API service for discovery
            Map<String, String> apiProperties = new HashMap<>();
            apiProperties.put("version", "1.0");
            apiProperties.put("service", "restaurant-system");
            apiProperties.put("api", "attendance");
            apiProperties.put("health", "/subscriber/health");
            apiProperties.put("discovery", "/subscriber/discovery");
            apiProperties.put("hostname", mdnsHostname);
            apiProperties.put("ip", localAddress.getHostAddress());

            apiServiceInfo = ServiceInfo.create(
                API_SERVICE_TYPE,
                SERVICE_NAME,
                serverPort,
                0, // weight
                0, // priority
                apiProperties
            );

            // 2. Register HTTP service with the fixed hostname
            Map<String, String> httpProperties = new HashMap<>();
            httpProperties.put("path", "/");
            httpProperties.put("api", "true");
            httpProperties.put("version", "1.0");
            httpProperties.put("txtvers", "1");

            // Use the hostname without .local suffix for the service name
            String hostServiceName = mdnsHostname.replace(".local", "");

            // Create service info with explicit hostname
            httpServiceInfo = ServiceInfo.create(
                HTTP_SERVICE_TYPE,
                hostServiceName,
                serverPort,
                0, // weight
                0, // priority
                true, // persistent
                httpProperties
            );

            // Register both services
            logger.info("📡 Registering API service...");
            jmdns.registerService(apiServiceInfo);

            logger.info("🌐 Registering hostname service...");
            jmdns.registerService(httpServiceInfo);

            // Wait for registration to complete
            Thread.sleep(2000);

            logger.info("✅ mDNS services registered successfully:");
            logger.info("   🔧 API Service: {} ({})", SERVICE_NAME, API_SERVICE_TYPE);
            logger.info("   🌐 Hostname Service: {} ({})", hostServiceName, HTTP_SERVICE_TYPE);
            logger.info("   🏠 Fixed Hostname: {}", mdnsHostname);
            logger.info("   🔌 Port: {}", serverPort);
            logger.info("   📍 IP Address: {}", currentIPAddress);
            logger.info("   🔗 Access URL: http://{}:{}/", mdnsHostname, serverPort);
            logger.info("   🔗 Fallback URL: http://{}:{}/", currentIPAddress, serverPort);

            isServiceRunning = true;

        } catch (Exception e) {
            logger.error("❌ Failed to start mDNS service: {}", e.getMessage(), e);
            isServiceRunning = false;
        }
    }
    
    /**
     * Stop mDNS service advertisement
     */
    public void stopService() {
        if (jmdns != null) {
            try {
                logger.info("🛑 Stopping mDNS services...");

                if (apiServiceInfo != null) {
                    logger.info("   Unregistering API service...");
                    jmdns.unregisterService(apiServiceInfo);
                }

                if (httpServiceInfo != null) {
                    logger.info("   Unregistering hostname service...");
                    jmdns.unregisterService(httpServiceInfo);
                }

                jmdns.close();
                logger.info("✅ mDNS services stopped successfully");

            } catch (IOException e) {
                logger.error("❌ Error stopping mDNS services: {}", e.getMessage(), e);
            } finally {
                jmdns = null;
                apiServiceInfo = null;
                httpServiceInfo = null;
                isServiceRunning = false;
            }
        }
    }
    
    /**
     * Get service information
     */
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();

        try {
            info.put("enabled", mdnsEnabled);
            info.put("hostname", mdnsHostname);
            info.put("port", serverPort);

            if (apiServiceInfo != null && httpServiceInfo != null) {
                info.put("registered", true);
                info.put("apiService", Map.of(
                    "name", apiServiceInfo.getName(),
                    "type", apiServiceInfo.getType(),
                    "port", apiServiceInfo.getPort()
                ));
                info.put("hostnameService", Map.of(
                    "name", httpServiceInfo.getName(),
                    "type", httpServiceInfo.getType(),
                    "port", httpServiceInfo.getPort()
                ));

                // Get local IP
                try {
                    InetAddress localAddress = InetAddress.getLocalHost();
                    info.put("localIP", localAddress.getHostAddress());
                    info.put("accessURL", "http://" + mdnsHostname + ":" + serverPort + "/");
                } catch (Exception e) {
                    info.put("localIP", "Error: " + e.getMessage());
                }

            } else {
                info.put("registered", false);
                info.put("reason", "Services not initialized");
            }

            return info;

        } catch (Exception e) {
            logger.error("Error getting service info: {}", e.getMessage(), e);
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("enabled", mdnsEnabled);
            errorInfo.put("registered", false);
            return errorInfo;
        }
    }
    
    /**
     * Check if service is running
     */
    public boolean isServiceRunning() {
        return jmdns != null && apiServiceInfo != null && httpServiceInfo != null;
    }

    /**
     * Get the mDNS hostname
     */
    public String getHostname() {
        return mdnsHostname;
    }

    /**
     * Test hostname resolution
     */
    public Map<String, Object> testHostnameResolution() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Try to resolve the hostname
            InetAddress resolved = InetAddress.getByName(mdnsHostname);
            result.put("hostname", mdnsHostname);
            result.put("resolved", true);
            result.put("resolvedIP", resolved.getHostAddress());
            result.put("isReachable", resolved.isReachable(5000));

        } catch (Exception e) {
            result.put("hostname", mdnsHostname);
            result.put("resolved", false);
            result.put("error", e.getMessage());

            // Add troubleshooting information
            result.put("troubleshooting", getTroubleshootingInfo());
        }

        return result;
    }

    /**
     * Get troubleshooting information for mDNS issues
     */
    public Map<String, Object> getTroubleshootingInfo() {
        Map<String, Object> info = new HashMap<>();

        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            info.put("localIP", localAddress.getHostAddress());
            info.put("targetHostname", mdnsHostname);
            info.put("expectedURL", "http://" + mdnsHostname + ":" + serverPort + "/");
            info.put("fallbackURL", "http://" + localAddress.getHostAddress() + ":" + serverPort + "/");

            // Check if Bonjour service is running
            info.put("bonjourInstructions", Map.of(
                "step1", "Ensure Bonjour Service is running in Windows Services",
                "step2", "Check Windows Firewall allows mDNS (port 5353 UDP)",
                "step3", "Try accessing via IP: http://" + localAddress.getHostAddress() + ":" + serverPort + "/",
                "step4", "Manual hosts file entry: " + localAddress.getHostAddress() + " " + mdnsHostname
            ));

            // Provide manual hosts file configuration
            String hostsFilePath = System.getenv("WINDIR") + "\\System32\\drivers\\etc\\hosts";
            String hostsEntry = localAddress.getHostAddress() + " " + mdnsHostname;

            info.put("manualConfiguration", Map.of(
                "hostsFile", hostsFilePath,
                "entryToAdd", hostsEntry,
                "instructions", "Add the entry to hosts file as Administrator"
            ));

        } catch (Exception e) {
            info.put("error", e.getMessage());
        }

        return info;
    }

    /**
     * Add hostname to Windows hosts file (requires admin privileges)
     */
    public Map<String, Object> addToHostsFile() {
        Map<String, Object> result = new HashMap<>();

        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            String hostsFilePath = System.getenv("WINDIR") + "\\System32\\drivers\\etc\\hosts";
            String hostsEntry = localAddress.getHostAddress() + " " + mdnsHostname;

            // Note: This would require admin privileges and file system access
            // For security reasons, we'll just provide instructions
            result.put("success", false);
            result.put("reason", "Requires administrator privileges");
            result.put("instructions", Map.of(
                "step1", "Run Command Prompt as Administrator",
                "step2", "Edit file: " + hostsFilePath,
                "step3", "Add line: " + hostsEntry,
                "step4", "Save file and test: ping " + mdnsHostname
            ));

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Get the best available local IP address for mDNS binding
     * Prioritizes non-loopback, site-local addresses
     */
    private InetAddress getBestLocalAddress() throws Exception {
        InetAddress bestAddress = null;
        InetAddress fallbackAddress = InetAddress.getLocalHost();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    // Skip IPv6 and loopback addresses
                    if (address.isLoopbackAddress() || address.getHostAddress().contains(":")) {
                        continue;
                    }

                    // Prefer site-local addresses (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
                    if (address.isSiteLocalAddress()) {
                        logger.debug("Found site-local address: {} on interface: {}",
                                   address.getHostAddress(), networkInterface.getName());
                        bestAddress = address;
                        break;
                    }

                    // Keep as fallback if no site-local found
                    if (bestAddress == null && !address.isLinkLocalAddress()) {
                        bestAddress = address;
                    }
                }

                if (bestAddress != null && bestAddress.isSiteLocalAddress()) {
                    break; // Found the best option
                }
            }

        } catch (SocketException e) {
            logger.warn("Failed to enumerate network interfaces: {}", e.getMessage());
        }

        InetAddress selectedAddress = bestAddress != null ? bestAddress : fallbackAddress;
        logger.info("Selected network address: {} ({})",
                   selectedAddress.getHostAddress(),
                   bestAddress != null ? "network interface" : "system default");

        return selectedAddress;
    }

    /**
     * Check if the current IP address has changed
     */
    public boolean hasNetworkChanged() {
        try {
            InetAddress currentBestAddress = getBestLocalAddress();
            String newIPAddress = currentBestAddress.getHostAddress();

            boolean hasChanged = currentIPAddress == null || !currentIPAddress.equals(newIPAddress);

            if (hasChanged) {
                logger.info("Network change detected: {} -> {}", currentIPAddress, newIPAddress);
            }

            return hasChanged;

        } catch (Exception e) {
            logger.error("Failed to check network change: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Restart mDNS services (useful after network changes)
     */
    public Map<String, Object> restartService() {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("🔄 Restarting mDNS services due to network change...");

            // Stop current service
            stopService();

            // Wait a moment
            Thread.sleep(2000);

            // Start service with new network configuration
            startService();

            result.put("success", true);
            result.put("message", "mDNS services restarted successfully");
            result.put("newIP", currentIPAddress);
            result.put("hostname", mdnsHostname);
            result.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            logger.error("Failed to restart mDNS services: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Get current service status including network information
     */
    public Map<String, Object> getNetworkStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            status.put("serviceRunning", isServiceRunning);
            status.put("currentIP", currentIPAddress);
            status.put("hostname", mdnsHostname);
            status.put("port", serverPort);

            // Get current best address
            InetAddress bestAddress = getBestLocalAddress();
            status.put("detectedIP", bestAddress.getHostAddress());
            status.put("networkChanged", hasNetworkChanged());

            // URLs
            status.put("hostnameURL", "http://" + mdnsHostname + ":" + serverPort + "/");
            status.put("ipURL", "http://" + bestAddress.getHostAddress() + ":" + serverPort + "/");

        } catch (Exception e) {
            status.put("error", e.getMessage());
        }

        return status;
    }
}
