package com.example.attendancesystem.subscriber.service;

import com.example.attendancesystem.config.ObservabilityConfig.CustomMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive Monitoring Service
 * Provides real-time monitoring, alerting, and performance tracking
 */
@Service
public class MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

    @Autowired
    private CustomMetrics customMetrics;

    // Performance tracking
    private final Map<String, PerformanceMetrics> endpointMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> alertCounters = new ConcurrentHashMap<>();
    
    // System health tracking
    private final AtomicLong systemStartTime = new AtomicLong(System.currentTimeMillis());
    private volatile boolean systemHealthy = true;
    private volatile String lastHealthCheckResult = "HEALTHY";

    /**
     * Performance metrics for endpoints
     */
    public static class PerformanceMetrics {
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private volatile long minResponseTime = Long.MAX_VALUE;
        private volatile long maxResponseTime = 0;
        private final LocalDateTime firstRequest = LocalDateTime.now();
        private volatile LocalDateTime lastRequest = LocalDateTime.now();

        public void recordRequest(long responseTime, boolean isError) {
            requestCount.incrementAndGet();
            if (isError) {
                errorCount.incrementAndGet();
            }
            
            totalResponseTime.addAndGet(responseTime);
            
            if (responseTime < minResponseTime) {
                minResponseTime = responseTime;
            }
            if (responseTime > maxResponseTime) {
                maxResponseTime = responseTime;
            }
            
            lastRequest = LocalDateTime.now();
        }

        public double getAverageResponseTime() {
            long count = requestCount.get();
            return count > 0 ? (double) totalResponseTime.get() / count : 0.0;
        }

        public double getErrorRate() {
            long count = requestCount.get();
            return count > 0 ? (double) errorCount.get() / count : 0.0;
        }

        // Getters
        public long getRequestCount() { return requestCount.get(); }
        public long getErrorCount() { return errorCount.get(); }
        public long getMinResponseTime() { return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime; }
        public long getMaxResponseTime() { return maxResponseTime; }
        public LocalDateTime getFirstRequest() { return firstRequest; }
        public LocalDateTime getLastRequest() { return lastRequest; }
    }

    /**
     * Record API request performance
     */
    public void recordApiRequest(String endpoint, String method, long responseTime, int statusCode, String userAgent) {
        // Add structured logging context
        MDC.put("endpoint", endpoint);
        MDC.put("method", method);
        MDC.put("response_time", String.valueOf(responseTime));
        MDC.put("status_code", String.valueOf(statusCode));
        MDC.put("user_agent", userAgent);
        MDC.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        try {
            // Update custom metrics
            customMetrics.incrementRequests();
            
            boolean isError = statusCode >= 400;
            if (isError) {
                customMetrics.incrementErrors();
            }

            // Update endpoint-specific metrics
            String endpointKey = method + " " + endpoint;
            PerformanceMetrics metrics = endpointMetrics.computeIfAbsent(endpointKey, k -> new PerformanceMetrics());
            metrics.recordRequest(responseTime, isError);

            // Log performance data
            if (isError) {
                logger.warn("❌ API Error: {} {} - Status: {}, Time: {}ms", method, endpoint, statusCode, responseTime);
            } else if (responseTime > 5000) { // Slow request threshold
                logger.warn("🐌 Slow Request: {} {} - Time: {}ms", method, endpoint, responseTime);
            } else {
                logger.debug("✅ API Request: {} {} - Status: {}, Time: {}ms", method, endpoint, statusCode, responseTime);
            }

            // Check for performance alerts
            checkPerformanceAlerts(endpointKey, metrics);

        } finally {
            MDC.clear();
        }
    }

    /**
     * Record security event
     */
    public void recordSecurityEvent(String eventType, String details, String sourceIp, String userAgent) {
        MDC.put("event_type", eventType);
        MDC.put("source_ip", sourceIp);
        MDC.put("user_agent", userAgent);
        MDC.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        try {
            securityLogger.warn("🔒 Security Event: {} - Details: {} - Source: {}", eventType, details, sourceIp);
            
            // Increment alert counter for this event type
            alertCounters.computeIfAbsent(eventType, k -> new AtomicLong(0)).incrementAndGet();
            
        } finally {
            MDC.clear();
        }
    }

    /**
     * Record system event
     */
    public void recordSystemEvent(String eventType, String message, Map<String, Object> context) {
        MDC.put("event_type", eventType);
        MDC.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Add context to MDC
        if (context != null) {
            context.forEach((key, value) -> MDC.put(key, String.valueOf(value)));
        }

        try {
            logger.info("🔧 System Event: {} - {}", eventType, message);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Check for performance alerts
     */
    private void checkPerformanceAlerts(String endpoint, PerformanceMetrics metrics) {
        // High error rate alert
        if (metrics.getRequestCount() > 10 && metrics.getErrorRate() > 0.1) { // More than 10% error rate
            String alertKey = "high_error_rate_" + endpoint;
            long alertCount = alertCounters.computeIfAbsent(alertKey, k -> new AtomicLong(0)).incrementAndGet();
            
            if (alertCount % 10 == 1) { // Alert every 10 occurrences
                logger.error("🚨 HIGH ERROR RATE ALERT: {} - Error Rate: {:.2f}% ({}/{})", 
                           endpoint, metrics.getErrorRate() * 100, metrics.getErrorCount(), metrics.getRequestCount());
            }
        }

        // Slow response time alert
        if (metrics.getAverageResponseTime() > 3000) { // More than 3 seconds average
            String alertKey = "slow_response_" + endpoint;
            long alertCount = alertCounters.computeIfAbsent(alertKey, k -> new AtomicLong(0)).incrementAndGet();
            
            if (alertCount % 5 == 1) { // Alert every 5 occurrences
                logger.warn("🐌 SLOW RESPONSE ALERT: {} - Average: {:.0f}ms, Max: {}ms", 
                          endpoint, metrics.getAverageResponseTime(), metrics.getMaxResponseTime());
            }
        }
    }

    /**
     * Get comprehensive system metrics
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // System uptime
        long uptime = System.currentTimeMillis() - systemStartTime.get();
        metrics.put("uptime_ms", uptime);
        metrics.put("uptime_hours", uptime / (1000 * 60 * 60));
        
        // Overall metrics
        metrics.put("total_requests", customMetrics.getTotalRequests());
        metrics.put("total_errors", customMetrics.getTotalErrors());
        metrics.put("active_connections", customMetrics.getActiveConnections());
        metrics.put("system_healthy", systemHealthy);
        metrics.put("last_health_check", lastHealthCheckResult);
        
        // Memory metrics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("max_mb", runtime.maxMemory() / 1024 / 1024);
        memory.put("total_mb", runtime.totalMemory() / 1024 / 1024);
        memory.put("free_mb", runtime.freeMemory() / 1024 / 1024);
        memory.put("used_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        metrics.put("memory", memory);
        
        // Endpoint metrics
        Map<String, Object> endpoints = new HashMap<>();
        endpointMetrics.forEach((endpoint, endpointMetrics) -> {
            Map<String, Object> endpointData = new HashMap<>();
            endpointData.put("request_count", endpointMetrics.getRequestCount());
            endpointData.put("error_count", endpointMetrics.getErrorCount());
            endpointData.put("error_rate", String.format("%.2f%%", endpointMetrics.getErrorRate() * 100));
            endpointData.put("avg_response_time", String.format("%.0f", endpointMetrics.getAverageResponseTime()));
            endpointData.put("min_response_time", endpointMetrics.getMinResponseTime());
            endpointData.put("max_response_time", endpointMetrics.getMaxResponseTime());
            endpointData.put("first_request", endpointMetrics.getFirstRequest().toString());
            endpointData.put("last_request", endpointMetrics.getLastRequest().toString());
            endpoints.put(endpoint, endpointData);
        });
        metrics.put("endpoints", endpoints);
        
        // Alert counters
        Map<String, Object> alerts = new HashMap<>();
        alertCounters.forEach((alertType, count) -> alerts.put(alertType, count.get()));
        metrics.put("alerts", alerts);
        
        metrics.put("timestamp", LocalDateTime.now().toString());
        
        return metrics;
    }

    /**
     * Periodic system health check
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void performHealthCheck() {
        try {
            // Check system health
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;
            
            // Check various health indicators
            boolean memoryHealthy = memoryUsage < 0.9; // Less than 90% memory usage
            boolean errorRateHealthy = customMetrics.getTotalRequests() == 0 || 
                                     (double) customMetrics.getTotalErrors() / customMetrics.getTotalRequests() < 0.05; // Less than 5% error rate
            
            systemHealthy = memoryHealthy && errorRateHealthy;
            
            if (systemHealthy) {
                lastHealthCheckResult = "HEALTHY";
                logger.debug("💚 System Health Check: HEALTHY - Memory: {:.1f}%, Error Rate: {:.2f}%", 
                           memoryUsage * 100, 
                           customMetrics.getTotalRequests() > 0 ? (double) customMetrics.getTotalErrors() / customMetrics.getTotalRequests() * 100 : 0);
            } else {
                lastHealthCheckResult = "UNHEALTHY";
                logger.warn("💔 System Health Check: UNHEALTHY - Memory: {:.1f}%, Error Rate: {:.2f}%", 
                          memoryUsage * 100, 
                          customMetrics.getTotalRequests() > 0 ? (double) customMetrics.getTotalErrors() / customMetrics.getTotalRequests() * 100 : 0);
            }
            
        } catch (Exception e) {
            systemHealthy = false;
            lastHealthCheckResult = "ERROR: " + e.getMessage();
            logger.error("❌ Health check failed", e);
        }
    }

    /**
     * Generate performance report
     */
    @Async
    public void generatePerformanceReport() {
        try {
            Map<String, Object> metrics = getSystemMetrics();
            performanceLogger.info("📊 Performance Report: {}", metrics);
        } catch (Exception e) {
            logger.error("❌ Failed to generate performance report", e);
        }
    }

    /**
     * Reset metrics (for testing purposes)
     */
    public void resetMetrics() {
        endpointMetrics.clear();
        alertCounters.clear();
        systemStartTime.set(System.currentTimeMillis());
        logger.info("🔄 Monitoring metrics reset");
    }
}
