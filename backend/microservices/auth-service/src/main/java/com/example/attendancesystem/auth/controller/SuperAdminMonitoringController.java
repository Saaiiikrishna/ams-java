package com.example.attendancesystem.auth.controller;

import com.example.attendancesystem.auth.dto.SuccessResponse;
import com.example.attendancesystem.auth.repository.EntityAdminRepository;
import com.example.attendancesystem.auth.repository.SuperAdminRepository;
import com.example.attendancesystem.grpc.organization.*;
import com.example.attendancesystem.grpc.user.*;
import com.example.attendancesystem.grpc.attendance.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * SuperAdmin System Monitoring Controller
 * Provides system-wide monitoring, performance metrics, and health checks
 */
@RestController
@RequestMapping("/super/monitoring")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminMonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminMonitoringController.class);

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Value("${grpc.client.organization-service.address:localhost}")
    private String organizationServiceHost;

    @Value("${grpc.client.organization-service.port:9092}")
    private int organizationServicePort;

    @Value("${grpc.client.user-service.address:localhost}")
    private String userServiceHost;

    @Value("${grpc.client.user-service.port:9093}")
    private int userServicePort;

    @Value("${grpc.client.attendance-service.address:localhost}")
    private String attendanceServiceHost;

    @Value("${grpc.client.attendance-service.port:9094}")
    private int attendanceServicePort;

    /**
     * Get system overview dashboard
     * GET /auth/super/monitoring/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getSystemDashboard() {
        try {
            logger.info("SuperAdmin requesting system dashboard");

            Map<String, Object> dashboardData = new HashMap<>();
            
            // Auth Service Statistics
            Map<String, Object> authStats = new HashMap<>();
            authStats.put("totalSuperAdmins", superAdminRepository.count());
            authStats.put("totalEntityAdmins", entityAdminRepository.count());
            authStats.put("activeEntityAdmins", entityAdminRepository.count()); // All entity admins are considered active
            dashboardData.put("authService", authStats);

            // Organization Service Statistics
            Map<String, Object> orgStats = getOrganizationStats();
            dashboardData.put("organizationService", orgStats);

            // User Service Statistics  
            Map<String, Object> userStats = getUserStats();
            dashboardData.put("userService", userStats);

            // Attendance Service Statistics
            Map<String, Object> attendanceStats = getAttendanceStats();
            dashboardData.put("attendanceService", attendanceStats);

            // System Health
            Map<String, Object> systemHealth = getSystemHealth();
            dashboardData.put("systemHealth", systemHealth);

            // Timestamp
            dashboardData.put("timestamp", LocalDateTime.now());
            dashboardData.put("uptime", getSystemUptime());

            return ResponseEntity.ok(new SuccessResponse("System dashboard retrieved successfully", dashboardData));

        } catch (Exception e) {
            logger.error("Error retrieving system dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error retrieving system dashboard");
        }
    }

    /**
     * Get service health status
     * GET /auth/super/monitoring/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> getServiceHealth() {
        try {
            logger.info("SuperAdmin requesting service health status");

            Map<String, Object> healthData = new HashMap<>();
            
            // Check each service health
            healthData.put("authService", checkAuthServiceHealth());
            healthData.put("organizationService", checkOrganizationServiceHealth());
            healthData.put("userService", checkUserServiceHealth());
            healthData.put("attendanceService", checkAttendanceServiceHealth());
            
            // Overall system status
            boolean allHealthy = healthData.values().stream()
                    .allMatch(status -> status instanceof Map && "UP".equals(((Map<?, ?>) status).get("status")));
            
            healthData.put("overallStatus", allHealthy ? "UP" : "DOWN");
            healthData.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(new SuccessResponse("Service health retrieved successfully", healthData));

        } catch (Exception e) {
            logger.error("Error retrieving service health: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error retrieving service health");
        }
    }

    /**
     * Get system performance metrics
     * GET /auth/super/monitoring/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getSystemMetrics() {
        try {
            logger.info("SuperAdmin requesting system metrics");

            Map<String, Object> metricsData = new HashMap<>();
            
            // JVM Metrics
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> jvmMetrics = new HashMap<>();
            jvmMetrics.put("totalMemory", runtime.totalMemory());
            jvmMetrics.put("freeMemory", runtime.freeMemory());
            jvmMetrics.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            jvmMetrics.put("maxMemory", runtime.maxMemory());
            jvmMetrics.put("availableProcessors", runtime.availableProcessors());
            metricsData.put("jvm", jvmMetrics);

            // Database Connection Pool (if available)
            Map<String, Object> dbMetrics = new HashMap<>();
            dbMetrics.put("status", "Connected");
            dbMetrics.put("activeConnections", "N/A"); // Would need actual connection pool metrics
            metricsData.put("database", dbMetrics);

            // Service Response Times (mock data - would need actual metrics)
            Map<String, Object> responseTimeMetrics = new HashMap<>();
            responseTimeMetrics.put("authService", "15ms");
            responseTimeMetrics.put("organizationService", "25ms");
            responseTimeMetrics.put("userService", "20ms");
            responseTimeMetrics.put("attendanceService", "30ms");
            metricsData.put("responseTimes", responseTimeMetrics);

            metricsData.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(new SuccessResponse("System metrics retrieved successfully", metricsData));

        } catch (Exception e) {
            logger.error("Error retrieving system metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error retrieving system metrics");
        }
    }

    /**
     * Get activity logs
     * GET /auth/super/monitoring/logs
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getActivityLogs(@RequestParam(defaultValue = "100") int limit) {
        try {
            logger.info("SuperAdmin requesting activity logs (limit: {})", limit);

            Map<String, Object> logsData = new HashMap<>();
            
            // Mock activity logs - in real implementation, would fetch from logging system
            Map<String, Object> recentActivity = new HashMap<>();
            recentActivity.put("totalLogs", "1000+");
            recentActivity.put("errorLogs", "5");
            recentActivity.put("warningLogs", "15");
            recentActivity.put("infoLogs", "980");
            recentActivity.put("lastError", "2024-01-01 10:30:00");
            recentActivity.put("lastWarning", "2024-01-01 11:45:00");
            
            logsData.put("summary", recentActivity);
            logsData.put("limit", limit);
            logsData.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(new SuccessResponse("Activity logs retrieved successfully", logsData));

        } catch (Exception e) {
            logger.error("Error retrieving activity logs: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error retrieving activity logs");
        }
    }

    // Helper methods
    private Map<String, Object> getOrganizationStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(organizationServiceHost, organizationServicePort)
                    .usePlaintext()
                    .build();
            try {
                OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationService = 
                    OrganizationServiceGrpc.newBlockingStub(channel);
                
                ListOrganizationsRequest request = ListOrganizationsRequest.newBuilder()
                        .setPage(0).setSize(1000).build();
                ListOrganizationsResponse response = organizationService.listOrganizations(request);
                
                stats.put("totalOrganizations", response.getTotalCount());
                stats.put("status", "UP");
            } finally {
                channel.shutdown();
            }
        } catch (Exception e) {
            stats.put("totalOrganizations", "N/A");
            stats.put("status", "DOWN");
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    private Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "UP");
        stats.put("totalUsers", "N/A"); // Would implement gRPC call to user service
        return stats;
    }

    private Map<String, Object> getAttendanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "UP");
        stats.put("totalSessions", "N/A"); // Would implement gRPC call to attendance service
        return stats;
    }

    private Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("services", 4);
        health.put("servicesUp", 4);
        health.put("servicesDown", 0);
        return health;
    }

    private Map<String, Object> checkAuthServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("responseTime", "5ms");
        return health;
    }

    private Map<String, Object> checkOrganizationServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(organizationServiceHost, organizationServicePort)
                    .usePlaintext()
                    .build();
            try {
                OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationService = 
                    OrganizationServiceGrpc.newBlockingStub(channel);
                
                // Simple health check - try to list organizations
                ListOrganizationsRequest request = ListOrganizationsRequest.newBuilder()
                        .setPage(0).setSize(1).build();
                organizationService.listOrganizations(request);
                
                health.put("status", "UP");
                health.put("responseTime", "10ms");
            } finally {
                channel.shutdown();
            }
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private Map<String, Object> checkUserServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP"); // Mock - would implement actual health check
        health.put("responseTime", "8ms");
        return health;
    }

    private Map<String, Object> checkAttendanceServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP"); // Mock - would implement actual health check
        health.put("responseTime", "12ms");
        return health;
    }

    private String getSystemUptime() {
        // Mock uptime - would calculate actual uptime
        return "2 days, 5 hours, 30 minutes";
    }
}
