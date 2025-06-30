package com.example.attendancesystem.subscriber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Enhanced Connection Retry Service with Circuit Breaker Pattern
 * Provides intelligent retry mechanisms with exponential backoff
 * and circuit breaker functionality for network operations
 */
@Service
public class ConnectionRetryService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionRetryService.class);

    // Circuit breaker states
    public enum CircuitState {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, failing fast
        HALF_OPEN  // Testing if service is back
    }

    // Configuration
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_DELAY_MS = 1000; // 1 second
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    private static final long DEFAULT_MAX_DELAY_MS = 30000; // 30 seconds
    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final long DEFAULT_RECOVERY_TIMEOUT_MS = 60000; // 1 minute

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentHashMap<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * Circuit breaker state for a specific service
     */
    private static class CircuitBreakerState {
        volatile CircuitState state = CircuitState.CLOSED;
        volatile int failureCount = 0;
        volatile LocalDateTime lastFailureTime;
        volatile LocalDateTime stateChangeTime = LocalDateTime.now();
        final int failureThreshold;
        final long recoveryTimeoutMs;

        CircuitBreakerState(int failureThreshold, long recoveryTimeoutMs) {
            this.failureThreshold = failureThreshold;
            this.recoveryTimeoutMs = recoveryTimeoutMs;
        }
    }

    /**
     * Retry configuration
     */
    public static class RetryConfig {
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private long initialDelayMs = DEFAULT_INITIAL_DELAY_MS;
        private double backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
        private long maxDelayMs = DEFAULT_MAX_DELAY_MS;
        private int failureThreshold = DEFAULT_FAILURE_THRESHOLD;
        private long recoveryTimeoutMs = DEFAULT_RECOVERY_TIMEOUT_MS;

        public RetryConfig maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public RetryConfig initialDelay(long delayMs) {
            this.initialDelayMs = delayMs;
            return this;
        }

        public RetryConfig backoffMultiplier(double multiplier) {
            this.backoffMultiplier = multiplier;
            return this;
        }

        public RetryConfig maxDelay(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public RetryConfig failureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }

        public RetryConfig recoveryTimeout(long timeoutMs) {
            this.recoveryTimeoutMs = timeoutMs;
            return this;
        }
    }

    /**
     * Execute operation with retry and circuit breaker
     */
    public <T> T executeWithRetry(String serviceName, Supplier<T> operation) {
        return executeWithRetry(serviceName, operation, new RetryConfig());
    }

    /**
     * Execute operation with custom retry configuration
     */
    public <T> T executeWithRetry(String serviceName, Supplier<T> operation, RetryConfig config) {
        CircuitBreakerState circuitBreaker = getOrCreateCircuitBreaker(serviceName, config);

        // Check circuit breaker state
        if (circuitBreaker.state == CircuitState.OPEN) {
            if (shouldAttemptRecovery(circuitBreaker)) {
                circuitBreaker.state = CircuitState.HALF_OPEN;
                circuitBreaker.stateChangeTime = LocalDateTime.now();
                logger.info("🔄 Circuit breaker for {} moved to HALF_OPEN state", serviceName);
            } else {
                throw new RuntimeException("Circuit breaker is OPEN for service: " + serviceName);
            }
        }

        Exception lastException = null;
        long delay = config.initialDelayMs;

        for (int attempt = 1; attempt <= config.maxRetries; attempt++) {
            try {
                logger.debug("🔄 Attempting operation for {} (attempt {}/{})", serviceName, attempt, config.maxRetries);
                
                T result = operation.get();
                
                // Success - reset circuit breaker
                onSuccess(circuitBreaker, serviceName);
                return result;

            } catch (Exception e) {
                lastException = e;
                logger.warn("❌ Operation failed for {} (attempt {}/{}): {}", 
                           serviceName, attempt, config.maxRetries, e.getMessage());

                // Record failure
                onFailure(circuitBreaker, serviceName);

                // If this is the last attempt or circuit is now open, don't wait
                if (attempt == config.maxRetries || circuitBreaker.state == CircuitState.OPEN) {
                    break;
                }

                // Wait before retry with exponential backoff
                try {
                    Thread.sleep(delay);
                    delay = Math.min((long) (delay * config.backoffMultiplier), config.maxDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        // All retries failed
        throw new RuntimeException("Operation failed after " + config.maxRetries + " attempts for service: " + serviceName, lastException);
    }

    /**
     * Execute operation asynchronously with retry
     */
    public <T> CompletableFuture<T> executeWithRetryAsync(String serviceName, Supplier<T> operation) {
        return executeWithRetryAsync(serviceName, operation, new RetryConfig());
    }

    /**
     * Execute operation asynchronously with custom retry configuration
     */
    public <T> CompletableFuture<T> executeWithRetryAsync(String serviceName, Supplier<T> operation, RetryConfig config) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(serviceName, operation, config));
    }

    /**
     * Get or create circuit breaker for service
     */
    private CircuitBreakerState getOrCreateCircuitBreaker(String serviceName, RetryConfig config) {
        return circuitBreakers.computeIfAbsent(serviceName, 
            k -> new CircuitBreakerState(config.failureThreshold, config.recoveryTimeoutMs));
    }

    /**
     * Handle successful operation
     */
    private void onSuccess(CircuitBreakerState circuitBreaker, String serviceName) {
        if (circuitBreaker.state == CircuitState.HALF_OPEN) {
            circuitBreaker.state = CircuitState.CLOSED;
            circuitBreaker.stateChangeTime = LocalDateTime.now();
            logger.info("✅ Circuit breaker for {} moved to CLOSED state", serviceName);
        }
        circuitBreaker.failureCount = 0;
    }

    /**
     * Handle failed operation
     */
    private void onFailure(CircuitBreakerState circuitBreaker, String serviceName) {
        circuitBreaker.failureCount++;
        circuitBreaker.lastFailureTime = LocalDateTime.now();

        if (circuitBreaker.state == CircuitState.HALF_OPEN) {
            // Failure in half-open state, go back to open
            circuitBreaker.state = CircuitState.OPEN;
            circuitBreaker.stateChangeTime = LocalDateTime.now();
            logger.warn("⚠️ Circuit breaker for {} moved back to OPEN state", serviceName);
        } else if (circuitBreaker.state == CircuitState.CLOSED && 
                   circuitBreaker.failureCount >= circuitBreaker.failureThreshold) {
            // Too many failures, open the circuit
            circuitBreaker.state = CircuitState.OPEN;
            circuitBreaker.stateChangeTime = LocalDateTime.now();
            logger.warn("🚨 Circuit breaker for {} moved to OPEN state after {} failures", 
                       serviceName, circuitBreaker.failureCount);
        }
    }

    /**
     * Check if we should attempt recovery from open state
     */
    private boolean shouldAttemptRecovery(CircuitBreakerState circuitBreaker) {
        long timeSinceStateChange = Duration.between(circuitBreaker.stateChangeTime, LocalDateTime.now()).toMillis();
        return timeSinceStateChange >= circuitBreaker.recoveryTimeoutMs;
    }

    /**
     * Get circuit breaker status for monitoring
     */
    public CircuitState getCircuitBreakerState(String serviceName) {
        CircuitBreakerState state = circuitBreakers.get(serviceName);
        return state != null ? state.state : CircuitState.CLOSED;
    }

    /**
     * Get circuit breaker statistics
     */
    public String getCircuitBreakerStats(String serviceName) {
        CircuitBreakerState state = circuitBreakers.get(serviceName);
        if (state == null) {
            return "No circuit breaker found for service: " + serviceName;
        }

        return String.format("Service: %s, State: %s, Failures: %d, Last Failure: %s, State Changed: %s",
                serviceName, state.state, state.failureCount, 
                state.lastFailureTime, state.stateChangeTime);
    }

    /**
     * Reset circuit breaker for a service
     */
    public void resetCircuitBreaker(String serviceName) {
        CircuitBreakerState state = circuitBreakers.get(serviceName);
        if (state != null) {
            state.state = CircuitState.CLOSED;
            state.failureCount = 0;
            state.stateChangeTime = LocalDateTime.now();
            logger.info("🔄 Circuit breaker for {} has been reset", serviceName);
        }
    }

    /**
     * Get all circuit breaker states for monitoring
     */
    public String getAllCircuitBreakerStats() {
        if (circuitBreakers.isEmpty()) {
            return "No circuit breakers registered";
        }

        StringBuilder stats = new StringBuilder("Circuit Breaker Status:\n");
        circuitBreakers.forEach((serviceName, state) -> {
            stats.append("- ").append(getCircuitBreakerStats(serviceName)).append("\n");
        });
        return stats.toString();
    }

    /**
     * Shutdown the service
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
