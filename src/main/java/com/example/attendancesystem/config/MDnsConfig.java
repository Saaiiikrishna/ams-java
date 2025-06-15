package com.example.attendancesystem.config;

import com.example.attendancesystem.service.MDnsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Configuration class for mDNS service
 * Handles lifecycle events for service discovery and network change monitoring
 */
@Component
public class MDnsConfig {

    private static final Logger logger = LoggerFactory.getLogger(MDnsConfig.class);

    @Autowired
    private MDnsService mdnsService;

    /**
     * Start mDNS service when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Application ready - starting mDNS service...");
        mdnsService.startService();
    }

    /**
     * Stop mDNS service when application shuts down
     */
    @PreDestroy
    public void onApplicationShutdown() {
        logger.info("Application shutting down - stopping mDNS service...");
        mdnsService.stopService();
    }

    /**
     * Periodically check for network changes and restart mDNS if needed
     * Runs every 2 minutes to detect network changes
     */
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    public void checkNetworkChanges() {
        try {
            if (mdnsService.hasNetworkChanged()) {
                logger.info("Network change detected - restarting mDNS services...");
                mdnsService.restartService();
            }
        } catch (Exception e) {
            logger.error("Error during network change check: {}", e.getMessage());
        }
    }
}
