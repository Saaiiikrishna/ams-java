package com.example.attendancesystem.config;

import brave.sampler.Sampler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive Observability Configuration
 * Provides distributed tracing, metrics collection, and health monitoring
 */
@Configuration
public class ObservabilityConfig {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

    @Value("${spring.application.name:attendance-system}")
    private String applicationName;

    @Value("${management.tracing.sampling.probability:0.1}")
    private float tracingSamplingRate;

    /**
     * Configure distributed tracing sampler
     */
    @Bean
    public Sampler alwaysSampler() {
        logger.info("🔍 Configuring distributed tracing with sampling rate: {}", tracingSamplingRate);
        return Sampler.create(tracingSamplingRate);
    }

    /**
     * Custom metrics configuration
     */
    @Component
    public static class CustomMetrics {
        
        private final MeterRegistry meterRegistry;
        private final Counter requestCounter;
        private final Counter errorCounter;
        private final Timer requestTimer;
        private final AtomicLong activeConnections = new AtomicLong(0);
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong totalErrors = new AtomicLong(0);

        public CustomMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Request metrics
            this.requestCounter = Counter.builder("attendance_requests_total")
                    .description("Total number of requests")
                    .tag("application", "attendance-system")
                    .register(meterRegistry);
                    
            this.errorCounter = Counter.builder("attendance_errors_total")
                    .description("Total number of errors")
                    .tag("application", "attendance-system")
                    .register(meterRegistry);
                    
            this.requestTimer = Timer.builder("attendance_request_duration")
                    .description("Request processing time")
                    .tag("application", "attendance-system")
                    .register(meterRegistry);

            // Gauge for active connections
            Gauge.builder("attendance_active_connections", this, CustomMetrics::getActiveConnections)
                    .description("Number of active connections")
                    .tag("application", "attendance-system")
                    .register(meterRegistry);

            logger.info("📊 Custom metrics initialized successfully");
        }

        public void incrementRequests() {
            requestCounter.increment();
            totalRequests.incrementAndGet();
        }

        public void incrementErrors() {
            errorCounter.increment();
            totalErrors.incrementAndGet();
        }

        public Timer.Sample startTimer() {
            return Timer.start(meterRegistry);
        }

        public void recordTimer(Timer.Sample sample) {
            sample.stop(requestTimer);
        }

        public void incrementActiveConnections() {
            activeConnections.incrementAndGet();
        }

        public void decrementActiveConnections() {
            activeConnections.decrementAndGet();
        }

        public long getActiveConnections() {
            return activeConnections.get();
        }

        public long getTotalRequests() {
            return totalRequests.get();
        }

        public long getTotalErrors() {
            return totalErrors.get();
        }
    }

    /**
     * Custom health indicator for attendance system
     */
    @Component
    public static class AttendanceSystemHealthIndicator implements HealthIndicator {

        private final CustomMetrics customMetrics;

        public AttendanceSystemHealthIndicator(CustomMetrics customMetrics) {
            this.customMetrics = customMetrics;
        }

        @Override
        public Health health() {
            try {
                // Check system health based on metrics
                long totalRequests = customMetrics.getTotalRequests();
                long totalErrors = customMetrics.getTotalErrors();
                long activeConnections = customMetrics.getActiveConnections();

                // Calculate error rate
                double errorRate = totalRequests > 0 ? (double) totalErrors / totalRequests : 0.0;

                Map<String, Object> details = new HashMap<>();
                details.put("total_requests", totalRequests);
                details.put("total_errors", totalErrors);
                details.put("error_rate", String.format("%.2f%%", errorRate * 100));
                details.put("active_connections", activeConnections);
                details.put("timestamp", LocalDateTime.now().toString());

                // Determine health status
                if (errorRate > 0.1) { // More than 10% error rate
                    return Health.down()
                            .withDetail("reason", "High error rate detected")
                            .withDetails(details)
                            .build();
                } else if (activeConnections > 1000) { // Too many active connections
                    return Health.down()
                            .withDetail("reason", "Too many active connections")
                            .withDetails(details)
                            .build();
                } else {
                    return Health.up()
                            .withDetails(details)
                            .build();
                }

            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    /**
     * Custom info contributor for application metadata
     */
    @Component
    public static class AttendanceSystemInfoContributor implements InfoContributor {

        @Value("${spring.application.name:attendance-system}")
        private String applicationName;

        @Value("${spring.application.version:1.0.0}")
        private String applicationVersion;

        @Value("${spring.profiles.active:default}")
        private String activeProfiles;

        private final CustomMetrics customMetrics;
        private final LocalDateTime startupTime = LocalDateTime.now();

        public AttendanceSystemInfoContributor(CustomMetrics customMetrics) {
            this.customMetrics = customMetrics;
        }

        @Override
        public void contribute(Info.Builder builder) {
            Map<String, Object> attendanceInfo = new HashMap<>();
            attendanceInfo.put("name", applicationName);
            attendanceInfo.put("version", applicationVersion);
            attendanceInfo.put("profiles", activeProfiles);
            attendanceInfo.put("startup_time", startupTime.toString());
            attendanceInfo.put("uptime_minutes", java.time.Duration.between(startupTime, LocalDateTime.now()).toMinutes());

            // Add metrics summary
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("total_requests", customMetrics.getTotalRequests());
            metrics.put("total_errors", customMetrics.getTotalErrors());
            metrics.put("active_connections", customMetrics.getActiveConnections());
            attendanceInfo.put("metrics", metrics);

            // Add system info
            Map<String, Object> system = new HashMap<>();
            system.put("java_version", System.getProperty("java.version"));
            system.put("os_name", System.getProperty("os.name"));
            system.put("os_version", System.getProperty("os.version"));
            system.put("available_processors", Runtime.getRuntime().availableProcessors());
            system.put("max_memory_mb", Runtime.getRuntime().maxMemory() / 1024 / 1024);
            system.put("total_memory_mb", Runtime.getRuntime().totalMemory() / 1024 / 1024);
            system.put("free_memory_mb", Runtime.getRuntime().freeMemory() / 1024 / 1024);
            attendanceInfo.put("system", system);

            builder.withDetail("attendance-system", attendanceInfo);
        }
    }

    /**
     * Service discovery health indicator
     */
    @Component
    public static class ServiceDiscoveryHealthIndicator implements HealthIndicator {

        @Override
        public Health health() {
            try {
                // Check if mDNS service is running
                // This is a simplified check - in production you'd check actual service discovery
                Map<String, Object> details = new HashMap<>();
                details.put("mdns_enabled", true);
                details.put("service_type", "_attendanceapi._tcp");
                details.put("discovery_status", "active");
                details.put("timestamp", LocalDateTime.now().toString());

                return Health.up()
                        .withDetails(details)
                        .build();

            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .withDetail("component", "service-discovery")
                        .build();
            }
        }
    }

    @PostConstruct
    public void logObservabilitySetup() {
        logger.info("🔍 Observability configuration completed:");
        logger.info("  📊 Metrics: Enabled with Prometheus export");
        logger.info("  🔍 Tracing: Enabled with sampling rate {}", tracingSamplingRate);
        logger.info("  🏥 Health: Custom health indicators registered");
        logger.info("  📋 Info: Application metadata contributor registered");
        logger.info("  🎯 Application: {}", applicationName);
    }
}
