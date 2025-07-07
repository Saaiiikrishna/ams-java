package com.example.attendancesystem.gateway.controller;

import com.example.attendancesystem.gateway.service.MDnsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for service discovery and mDNS status
 */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    @Autowired
    private MDnsService mdnsService;

    /**
     * Get mDNS service status
     */
    @GetMapping("/mdns")
    public ResponseEntity<Map<String, Object>> getMDnsStatus() {
        Map<String, Object> status = mdnsService.getServiceInfo();
        return ResponseEntity.ok(status);
    }

    /**
     * Health check endpoint for discovery
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDiscoveryHealth() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "api-gateway",
            "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(health);
    }
}
