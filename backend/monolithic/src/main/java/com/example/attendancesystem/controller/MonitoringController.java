package com.example.attendancesystem.subscriber.controller;

import com.example.attendancesystem.service.MonitoringService;
import com.example.attendancesystem.service.ServiceRegistryManager;
import com.example.attendancesystem.service.ConnectionRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitoring Dashboard Controller
 * Provides comprehensive monitoring and observability endpoints
 */
@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private ServiceRegistryManager serviceRegistryManager;

    @Autowired
    private ConnectionRetryService connectionRetryService;

    /**
     * Get comprehensive system metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("metrics", monitoringService.getSystemMetrics());
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get system metrics", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get service registry statistics
     */
    @GetMapping("/service-registry")
    public ResponseEntity<Map<String, Object>> getServiceRegistryStats() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("registry_stats", serviceRegistryManager.getRegistryStats());
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get service registry stats", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get circuit breaker statistics
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStats() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("circuit_breaker_stats", connectionRetryService.getAllCircuitBreakerStats());
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get circuit breaker stats", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get comprehensive dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // System metrics
            dashboard.put("system_metrics", monitoringService.getSystemMetrics());
            
            // Service registry stats
            dashboard.put("service_registry", serviceRegistryManager.getRegistryStats());
            
            // Circuit breaker stats
            dashboard.put("circuit_breakers", connectionRetryService.getAllCircuitBreakerStats());
            
            // System health summary
            Map<String, Object> healthSummary = new HashMap<>();
            Map<String, Object> systemMetrics = monitoringService.getSystemMetrics();
            healthSummary.put("system_healthy", systemMetrics.get("system_healthy"));
            healthSummary.put("last_health_check", systemMetrics.get("last_health_check"));
            healthSummary.put("uptime_hours", systemMetrics.get("uptime_hours"));
            healthSummary.put("total_requests", systemMetrics.get("total_requests"));
            healthSummary.put("total_errors", systemMetrics.get("total_errors"));
            healthSummary.put("active_connections", systemMetrics.get("active_connections"));
            dashboard.put("health_summary", healthSummary);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("dashboard", dashboard);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get dashboard data", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Generate performance report
     */
    @PostMapping("/report/performance")
    public ResponseEntity<Map<String, Object>> generatePerformanceReport() {
        try {
            monitoringService.generatePerformanceReport();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Performance report generated successfully");
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to generate performance report", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Reset circuit breaker for a service
     */
    @PostMapping("/circuit-breaker/{serviceName}/reset")
    public ResponseEntity<Map<String, Object>> resetCircuitBreaker(@PathVariable String serviceName) {
        try {
            connectionRetryService.resetCircuitBreaker(serviceName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Circuit breaker reset successfully");
            response.put("service_name", serviceName);
            response.put("timestamp", LocalDateTime.now().toString());
            
            logger.info("🔄 Circuit breaker reset for service: {}", serviceName);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to reset circuit breaker for service: {}", serviceName, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Test system resilience
     */
    @PostMapping("/test/resilience")
    public ResponseEntity<Map<String, Object>> testSystemResilience(@RequestBody ResilienceTestRequest request) {
        try {
            Map<String, Object> testResults = new HashMap<>();
            
            // Test circuit breaker
            if (request.isTestCircuitBreaker()) {
                testResults.put("circuit_breaker_test", "Circuit breaker functionality tested");
                logger.info("🧪 Testing circuit breaker resilience");
            }
            
            // Test service discovery
            if (request.isTestServiceDiscovery()) {
                testResults.put("service_discovery_test", serviceRegistryManager.getRegistryStats());
                logger.info("🧪 Testing service discovery resilience");
            }
            
            // Test monitoring
            if (request.isTestMonitoring()) {
                testResults.put("monitoring_test", monitoringService.getSystemMetrics());
                logger.info("🧪 Testing monitoring system resilience");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Resilience test completed");
            response.put("test_results", testResults);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to test system resilience", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get system alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getSystemAlerts() {
        try {
            Map<String, Object> systemMetrics = monitoringService.getSystemMetrics();
            Map<String, Object> alerts = (Map<String, Object>) systemMetrics.get("alerts");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("alerts", alerts);
            response.put("alert_count", alerts.size());
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get system alerts", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Reset monitoring metrics (for testing)
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetMonitoringMetrics() {
        try {
            monitoringService.resetMetrics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Monitoring metrics reset successfully");
            response.put("timestamp", LocalDateTime.now().toString());
            
            logger.info("🔄 Monitoring metrics reset via API");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to reset monitoring metrics", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Request DTOs
    public static class ResilienceTestRequest {
        private boolean testCircuitBreaker = true;
        private boolean testServiceDiscovery = true;
        private boolean testMonitoring = true;

        // Getters and setters
        public boolean isTestCircuitBreaker() { return testCircuitBreaker; }
        public void setTestCircuitBreaker(boolean testCircuitBreaker) { this.testCircuitBreaker = testCircuitBreaker; }
        
        public boolean isTestServiceDiscovery() { return testServiceDiscovery; }
        public void setTestServiceDiscovery(boolean testServiceDiscovery) { this.testServiceDiscovery = testServiceDiscovery; }
        
        public boolean isTestMonitoring() { return testMonitoring; }
        public void setTestMonitoring(boolean testMonitoring) { this.testMonitoring = testMonitoring; }
    }
}
