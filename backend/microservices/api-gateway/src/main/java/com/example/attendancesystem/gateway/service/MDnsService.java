package com.example.attendancesystem.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * mDNS Service for API Gateway
 * Registers the API Gateway as restaurant.local for backward compatibility
 */
@Service
public class MDnsService {

    private static final Logger logger = LoggerFactory.getLogger(MDnsService.class);

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
    private volatile boolean isServiceRunning = false;

    @PostConstruct
    public void startService() {
        if (!mdnsEnabled) {
            logger.info("mDNS service is disabled");
            return;
        }

        try {
            logger.info("🌐 Starting mDNS service registration for API Gateway...");
            logger.info("   📡 Service Name: {}", SERVICE_NAME);
            logger.info("   🏠 Hostname: {}", mdnsHostname);
            logger.info("   🚪 Port: {}", serverPort);

            // Get local IP address
            InetAddress localAddress = InetAddress.getLocalHost();
            logger.info("   📍 Local IP: {}", localAddress.getHostAddress());

            // Create JmDNS instance
            jmdns = JmDNS.create(localAddress, "APIGateway-" + localAddress.getHostAddress());

            // Wait for JmDNS to initialize
            Thread.sleep(1000);

            // Register API service for discovery
            Map<String, String> apiProperties = new HashMap<>();
            apiProperties.put("version", "1.0");
            apiProperties.put("service", "restaurant-system");
            apiProperties.put("api", "gateway");
            apiProperties.put("health", "/actuator/health");
            apiProperties.put("hostname", mdnsHostname);
            apiProperties.put("ip", localAddress.getHostAddress());
            apiProperties.put("type", "api-gateway");

            apiServiceInfo = ServiceInfo.create(
                API_SERVICE_TYPE,
                SERVICE_NAME,
                serverPort,
                0, // weight
                0, // priority
                apiProperties
            );

            // Register HTTP service for hostname resolution
            Map<String, String> httpProperties = new HashMap<>();
            httpProperties.put("path", "/");
            httpProperties.put("hostname", mdnsHostname);
            httpProperties.put("ip", localAddress.getHostAddress());
            httpProperties.put("type", "api-gateway");

            httpServiceInfo = ServiceInfo.create(
                HTTP_SERVICE_TYPE,
                mdnsHostname,
                serverPort,
                0, // weight
                0, // priority
                httpProperties
            );

            // Register both services
            jmdns.registerService(apiServiceInfo);
            jmdns.registerService(httpServiceInfo);

            isServiceRunning = true;

            logger.info("✅ mDNS service registration completed successfully!");
            logger.info("   🌐 API Gateway accessible at: http://{}:{}/", mdnsHostname, serverPort);
            logger.info("   📱 Mobile apps can now discover the service");

        } catch (Exception e) {
            logger.error("❌ Failed to start mDNS service", e);
            isServiceRunning = false;
        }
    }

    @PreDestroy
    public void stopService() {
        if (jmdns != null) {
            try {
                logger.info("🛑 Stopping mDNS service...");
                
                if (apiServiceInfo != null) {
                    jmdns.unregisterService(apiServiceInfo);
                }
                
                if (httpServiceInfo != null) {
                    jmdns.unregisterService(httpServiceInfo);
                }
                
                jmdns.close();
                isServiceRunning = false;
                
                logger.info("✅ mDNS service stopped successfully");
            } catch (IOException e) {
                logger.error("❌ Error stopping mDNS service", e);
            }
        }
    }

    /**
     * Get mDNS service status information
     */
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try {
            info.put("enabled", mdnsEnabled);
            info.put("hostname", mdnsHostname);
            info.put("port", serverPort);
            info.put("running", isServiceRunning);

            if (apiServiceInfo != null && httpServiceInfo != null) {
                info.put("registered", true);
                info.put("apiService", Map.of(
                    "name", apiServiceInfo.getName(),
                    "type", apiServiceInfo.getType(),
                    "port", apiServiceInfo.getPort()
                ));
                info.put("httpService", Map.of(
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
            }
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }

        return info;
    }
}
