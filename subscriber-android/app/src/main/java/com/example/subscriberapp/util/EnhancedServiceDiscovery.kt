package com.example.subscriberapp.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Enhanced Service Discovery with intelligent load balancing,
 * health monitoring, and circuit breaker patterns
 */
class EnhancedServiceDiscovery(private val context: Context) {
    
    companion object {
        private const val TAG = "EnhancedServiceDiscovery"
        private const val HEALTH_CHECK_INTERVAL = 30000L // 30 seconds
        private const val CIRCUIT_BREAKER_FAILURE_THRESHOLD = 3
        private const val CIRCUIT_BREAKER_RECOVERY_TIMEOUT = 60000L // 1 minute
        private const val LOAD_BALANCER_WEIGHT_DECAY = 0.9
    }

    // Service instance data class
    data class ServiceInstance(
        val id: String,
        val baseUrl: String,
        val host: String,
        val port: Int,
        var weight: Double = 1.0,
        var responseTime: Long = 0L,
        var lastHealthCheck: Long = 0L,
        var isHealthy: Boolean = true,
        var failureCount: Int = 0,
        var circuitState: CircuitState = CircuitState.CLOSED,
        var lastFailureTime: Long = 0L,
        val metadata: Map<String, String> = emptyMap()
    )

    enum class CircuitState {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, failing fast
        HALF_OPEN  // Testing if service is back
    }

    // Service registry
    private val serviceInstances = ConcurrentHashMap<String, ServiceInstance>()
    private val healthCheckClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Coroutine scope for background tasks
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Health monitoring flow
    private val _healthUpdates = MutableSharedFlow<ServiceInstance>()
    val healthUpdates: SharedFlow<ServiceInstance> = _healthUpdates.asSharedFlow()

    // Service discovery flow
    private val _serviceUpdates = MutableSharedFlow<List<ServiceInstance>>()
    val serviceUpdates: SharedFlow<List<ServiceInstance>> = _serviceUpdates.asSharedFlow()

    init {
        startHealthMonitoring()
        startServiceDiscovery()
    }

    /**
     * Start continuous service discovery
     */
    private fun startServiceDiscovery() {
        scope.launch {
            while (isActive) {
                try {
                    discoverServices()
                    delay(60000) // Discover every minute
                } catch (e: Exception) {
                    Log.e(TAG, "Service discovery error: ${e.message}")
                    delay(30000) // Retry in 30 seconds on error
                }
            }
        }
    }

    /**
     * Discover available services
     */
    private suspend fun discoverServices() {
        Log.d(TAG, "🔍 Starting enhanced service discovery...")
        
        try {
            // Use existing mDNS discovery
            val mdnsDiscovery = MDnsDiscovery(context)
            val discoveredServices = mdnsDiscovery.discoverServicesSync(10000L)
            
            // Convert to enhanced service instances
            val newInstances = discoveredServices.mapIndexed { index, service ->
                val instanceId = "${service.host}:${service.port}"
                ServiceInstance(
                    id = instanceId,
                    baseUrl = service.baseUrl,
                    host = service.host,
                    port = service.port,
                    metadata = mapOf(
                        "name" => service.name,
                        "discovered_at" => System.currentTimeMillis().toString(),
                        "discovery_method" => "mdns"
                    )
                )
            }

            // Update service registry
            updateServiceRegistry(newInstances)
            
            Log.i(TAG, "✅ Discovered ${newInstances.size} service instances")
            _serviceUpdates.emit(getHealthyInstances())
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Service discovery failed: ${e.message}")
        }
    }

    /**
     * Update service registry with new instances
     */
    private fun updateServiceRegistry(newInstances: List<ServiceInstance>) {
        // Remove instances that are no longer discovered
        val discoveredIds = newInstances.map { it.id }.toSet()
        serviceInstances.keys.removeAll { id -> id !in discoveredIds }
        
        // Add or update instances
        newInstances.forEach { instance ->
            val existing = serviceInstances[instance.id]
            if (existing != null) {
                // Update existing instance, preserve health state
                existing.baseUrl = instance.baseUrl
                existing.host = instance.host
                existing.port = instance.port
            } else {
                // Add new instance
                serviceInstances[instance.id] = instance
                Log.i(TAG, "➕ Added new service instance: ${instance.id}")
            }
        }
    }

    /**
     * Start health monitoring for all services
     */
    private fun startHealthMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    checkAllServicesHealth()
                    delay(HEALTH_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Health monitoring error: ${e.message}")
                    delay(10000) // Retry in 10 seconds on error
                }
            }
        }
    }

    /**
     * Check health of all service instances
     */
    private suspend fun checkAllServicesHealth() {
        val instances = serviceInstances.values.toList()
        if (instances.isEmpty()) return

        Log.d(TAG, "🏥 Checking health of ${instances.size} service instances...")

        instances.forEach { instance ->
            scope.launch {
                checkServiceHealth(instance)
            }
        }
    }

    /**
     * Check health of a specific service instance
     */
    private suspend fun checkServiceHealth(instance: ServiceInstance) {
        try {
            val startTime = System.currentTimeMillis()
            val healthUrl = "${instance.baseUrl}subscriber/health"
            
            val request = Request.Builder()
                .url(healthUrl)
                .get()
                .build()

            val response = healthCheckClient.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime
            val isHealthy = response.isSuccessful

            // Update instance health
            instance.lastHealthCheck = System.currentTimeMillis()
            instance.responseTime = responseTime
            
            if (isHealthy) {
                onHealthCheckSuccess(instance)
            } else {
                onHealthCheckFailure(instance, "HTTP ${response.code}")
            }

            response.close()
            _healthUpdates.emit(instance)

        } catch (e: Exception) {
            onHealthCheckFailure(instance, e.message ?: "Unknown error")
            _healthUpdates.emit(instance)
        }
    }

    /**
     * Handle successful health check
     */
    private fun onHealthCheckSuccess(instance: ServiceInstance) {
        val wasUnhealthy = !instance.isHealthy
        
        instance.isHealthy = true
        instance.failureCount = 0
        
        // Update circuit breaker state
        when (instance.circuitState) {
            CircuitState.HALF_OPEN -> {
                instance.circuitState = CircuitState.CLOSED
                Log.i(TAG, "🔄 Circuit breaker CLOSED for ${instance.id}")
            }
            CircuitState.OPEN -> {
                // Should not happen, but reset anyway
                instance.circuitState = CircuitState.CLOSED
            }
            CircuitState.CLOSED -> {
                // Already closed, no change needed
            }
        }

        // Increase weight for good performance
        if (instance.responseTime < 1000) { // Less than 1 second
            instance.weight = minOf(instance.weight * 1.1, 2.0)
        }

        if (wasUnhealthy) {
            Log.i(TAG, "✅ Service ${instance.id} is back online")
        }
    }

    /**
     * Handle failed health check
     */
    private fun onHealthCheckFailure(instance: ServiceInstance, error: String) {
        instance.isHealthy = false
        instance.failureCount++
        instance.lastFailureTime = System.currentTimeMillis()
        
        // Decrease weight
        instance.weight *= LOAD_BALANCER_WEIGHT_DECAY

        // Update circuit breaker state
        if (instance.failureCount >= CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
            when (instance.circuitState) {
                CircuitState.CLOSED -> {
                    instance.circuitState = CircuitState.OPEN
                    Log.w(TAG, "🚨 Circuit breaker OPEN for ${instance.id} after ${instance.failureCount} failures")
                }
                CircuitState.HALF_OPEN -> {
                    instance.circuitState = CircuitState.OPEN
                    Log.w(TAG, "🚨 Circuit breaker back to OPEN for ${instance.id}")
                }
                CircuitState.OPEN -> {
                    // Check if we should try half-open
                    val timeSinceLastFailure = System.currentTimeMillis() - instance.lastFailureTime
                    if (timeSinceLastFailure > CIRCUIT_BREAKER_RECOVERY_TIMEOUT) {
                        instance.circuitState = CircuitState.HALF_OPEN
                        Log.i(TAG, "🔄 Circuit breaker HALF_OPEN for ${instance.id}")
                    }
                }
            }
        }

        Log.w(TAG, "❌ Health check failed for ${instance.id}: $error")
    }

    /**
     * Get the best available service instance using load balancing
     */
    fun getBestServiceInstance(): ServiceInstance? {
        val healthyInstances = getHealthyInstances()
        if (healthyInstances.isEmpty()) {
            Log.w(TAG, "⚠️ No healthy service instances available")
            return null
        }

        // Weighted random selection based on performance
        val totalWeight = healthyInstances.sumOf { it.weight }
        if (totalWeight <= 0) {
            // Fallback to random selection
            return healthyInstances.random()
        }

        val randomValue = Random.nextDouble() * totalWeight
        var currentWeight = 0.0

        for (instance in healthyInstances) {
            currentWeight += instance.weight
            if (randomValue <= currentWeight) {
                Log.d(TAG, "🎯 Selected service instance: ${instance.id} (weight: ${instance.weight})")
                return instance
            }
        }

        // Fallback to first instance
        return healthyInstances.first()
    }

    /**
     * Get all healthy service instances
     */
    fun getHealthyInstances(): List<ServiceInstance> {
        return serviceInstances.values.filter { 
            it.isHealthy && it.circuitState != CircuitState.OPEN 
        }
    }

    /**
     * Get all service instances (including unhealthy)
     */
    fun getAllInstances(): List<ServiceInstance> {
        return serviceInstances.values.toList()
    }

    /**
     * Get service statistics
     */
    fun getServiceStats(): Map<String, Any> {
        val instances = serviceInstances.values
        return mapOf(
            "total_instances" to instances.size,
            "healthy_instances" to instances.count { it.isHealthy },
            "circuit_breaker_open" to instances.count { it.circuitState == CircuitState.OPEN },
            "circuit_breaker_half_open" to instances.count { it.circuitState == CircuitState.HALF_OPEN },
            "average_response_time" to instances.filter { it.isHealthy }.map { it.responseTime }.average().takeIf { !it.isNaN() } ?: 0.0,
            "last_discovery" to System.currentTimeMillis()
        )
    }

    /**
     * Force refresh of service discovery
     */
    suspend fun forceRefresh() {
        Log.i(TAG, "🔄 Forcing service discovery refresh...")
        discoverServices()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        healthCheckClient.dispatcher.executorService.shutdown()
    }
}
