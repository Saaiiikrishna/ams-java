package com.example.entityadmin.util

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

/**
 * Enhanced mDNS discovery for Entity Admin app
 * Discovers backend servers advertising the attendance API service
 */
class EnhancedMDnsDiscovery(private val context: Context) {
    
    companion object {
        private const val TAG = "EntityMDnsDiscovery"
        private const val SERVICE_TYPE = "_grpc._tcp.local."
        private const val DISCOVERY_TIMEOUT_MS = 8000L
        private const val SERVICE_NAME_FILTER = "attendance-system"
    }
    
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    private val discoveredServices = ConcurrentHashMap<String, DiscoveredService>()
    private var jmdns: JmDNS? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    
    data class DiscoveredService(
        val name: String,
        val host: String,
        val port: Int,
        val properties: Map<String, String> = emptyMap()
    ) {
        // Convert gRPC port (9090) to REST port (8080) for API calls
        val restPort: Int get() = if (port == 9090) 8080 else port
        val baseUrl: String get() = "http://$host:$restPort/"
        val healthUrl: String get() = "${baseUrl}actuator/health"
        val entityHealthUrl: String get() = "${baseUrl}entity/health"
        val grpcUrl: String get() = "$host:$port"
        
        fun isAttendanceSystem(): Boolean {
            return name.contains("attendance", ignoreCase = true) ||
                   name.contains("grpc", ignoreCase = true) ||
                   properties.containsKey("service") && 
                   properties["service"]?.contains("attendance", ignoreCase = true) == true
        }
    }
    
    /**
     * Discover services using JmDNS with Flow-based results
     */
    fun discoverServices(): Flow<List<DiscoveredService>> = flow {
        Log.i(TAG, "🚀 Starting entity admin mDNS service discovery...")
        
        try {
            withTimeout(DISCOVERY_TIMEOUT_MS) {
                startJmDNSDiscovery()
                
                // Emit initial empty list
                emit(emptyList())
                
                // Wait and collect results
                var lastEmittedSize = 0
                repeat(8) { // Check 8 times over 8 seconds
                    delay(1000)
                    val currentServices = discoveredServices.values.toList()
                    if (currentServices.size != lastEmittedSize) {
                        emit(currentServices)
                        lastEmittedSize = currentServices.size
                        Log.i(TAG, "📡 Entity admin emitted ${currentServices.size} discovered services")
                    }
                }
                
                // Final emit
                emit(discoveredServices.values.toList())
            }
        } catch (e: TimeoutCancellationException) {
            Log.i(TAG, "⏰ Entity admin discovery timeout reached, found ${discoveredServices.size} services")
            emit(discoveredServices.values.toList())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Entity admin enhanced discovery failed: ${e.message}", e)
            emit(emptyList())
        } finally {
            cleanup()
        }
    }
    
    /**
     * Discover services synchronously with timeout
     */
    suspend fun discoverServicesSync(timeoutMs: Long = DISCOVERY_TIMEOUT_MS): List<DiscoveredService> {
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(timeoutMs) {
                    startJmDNSDiscovery()
                    
                    // Wait for discovery
                    delay(minOf(timeoutMs, 6000L))
                    
                    val services = discoveredServices.values.toList()
                    Log.i(TAG, "🎯 Entity admin sync discovery completed: ${services.size} services found")
                    services
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Entity admin sync enhanced discovery failed: ${e.message}", e)
                emptyList()
            } finally {
                cleanup()
            }
        }
    }
    
    private suspend fun startJmDNSDiscovery() = withContext(Dispatchers.IO) {
        try {
            // Acquire multicast lock
            acquireMulticastLock()
            
            // Get WiFi IP address
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress
            val inetAddress = InetAddress.getByAddress(
                byteArrayOf(
                    (ipAddress and 0xff).toByte(),
                    (ipAddress shr 8 and 0xff).toByte(),
                    (ipAddress shr 16 and 0xff).toByte(),
                    (ipAddress shr 24 and 0xff).toByte()
                )
            )
            
            Log.i(TAG, "📱 Entity admin using WiFi IP: ${inetAddress.hostAddress}")
            
            // Create JmDNS instance
            jmdns = JmDNS.create(inetAddress, "EntityAdminClient")
            
            // Clear previous discoveries
            discoveredServices.clear()
            
            // Create service listener
            val serviceListener = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    Log.d(TAG, "🔍 Entity admin service added: ${event.name}")
                    // Request service info
                    jmdns?.requestServiceInfo(event.type, event.name, 3000)
                }
                
                override fun serviceRemoved(event: ServiceEvent) {
                    Log.d(TAG, "🗑️ Entity admin service removed: ${event.name}")
                    discoveredServices.remove(event.name)
                }
                
                override fun serviceResolved(event: ServiceEvent) {
                    val info = event.info
                    Log.i(TAG, "✅ Entity admin service resolved: ${info.name}")
                    Log.i(TAG, "   📍 Host: ${info.hostAddresses?.firstOrNull()}")
                    Log.i(TAG, "   🔌 Port: ${info.port}")
                    Log.i(TAG, "   📋 Type: ${info.type}")
                    
                    // Filter for our attendance system service
                    if (info.name.contains(SERVICE_NAME_FILTER, ignoreCase = true) ||
                        info.name.contains("attendance", ignoreCase = true)) {
                        
                        val hostAddress = info.hostAddresses?.firstOrNull()
                        if (hostAddress != null && info.port > 0) {
                            
                            // Parse properties
                            val properties = mutableMapOf<String, String>()
                            info.propertyNames?.forEach { key ->
                                val value = info.getPropertyString(key)
                                if (value != null) {
                                    properties[key] = value
                                }
                            }
                            
                            val service = DiscoveredService(
                                name = info.name,
                                host = hostAddress,
                                port = info.port,
                                properties = properties
                            )
                            
                            discoveredServices[info.name] = service
                            
                            Log.i(TAG, "🎉 Entity admin added attendance service: ${service.name}")
                            Log.i(TAG, "   🌐 REST URL: ${service.baseUrl}")
                            Log.i(TAG, "   📡 gRPC URL: ${service.grpcUrl}")
                            Log.i(TAG, "   ❤️ Health URL: ${service.healthUrl}")
                        }
                    } else {
                        Log.d(TAG, "⏭️ Entity admin skipping non-attendance service: ${info.name}")
                    }
                }
            }
            
            // Start listening for services
            jmdns?.addServiceListener(SERVICE_TYPE, serviceListener)
            Log.i(TAG, "👂 Entity admin started listening for services of type: $SERVICE_TYPE")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Entity admin failed to start JmDNS discovery: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Get currently discovered services
     */
    fun getDiscoveredServices(): List<DiscoveredService> {
        return discoveredServices.values.toList()
    }
    
    /**
     * Acquire multicast lock for mDNS
     */
    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("EntityMDnsDiscovery")
                multicastLock?.setReferenceCounted(true)
            }
            
            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
                Log.i(TAG, "🔓 Entity admin multicast lock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Entity admin failed to acquire multicast lock: ${e.message}", e)
        }
    }
    
    /**
     * Release multicast lock
     */
    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.i(TAG, "🔒 Entity admin multicast lock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Entity admin failed to release multicast lock: ${e.message}", e)
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            jmdns?.close()
            jmdns = null
            releaseMulticastLock()
            multicastLock = null
            Log.i(TAG, "🧹 Entity admin cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Entity admin cleanup failed: ${e.message}", e)
        }
    }
}
