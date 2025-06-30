package com.example.entityadmin.util

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * mDNS service discovery using Android's NsdManager
 * Discovers backend servers advertising the attendance API service
 */
class MDnsDiscovery(private val context: Context) {
    
    companion object {
        private const val TAG = "EntityMDnsDiscovery"
        private const val SERVICE_TYPE = "_attendanceapi._tcp"
        private const val DISCOVERY_TIMEOUT_MS = 5000L // Reduced to 5 seconds for faster discovery
        private const val FAST_DISCOVERY_TIMEOUT_MS = 3000L // 3 seconds for fast discovery
    }
    
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val discoveredServices = ConcurrentHashMap<String, DiscoveredService>()
    private val isDiscovering = AtomicBoolean(false)
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    
    data class DiscoveredService(
        val name: String,
        val host: String,
        val port: Int,
        val properties: Map<String, String> = emptyMap()
    ) {
        val baseUrl: String get() = "http://$host:$port/"
        val healthUrl: String get() = "${baseUrl}api/entity/health"  // Entity admin health endpoint
    }
    
    /**
     * Discover services using mDNS with Flow-based results
     */
    fun discoverServices(): Flow<List<DiscoveredService>> = flow {
        Log.i(TAG, "Starting mDNS service discovery...")
        
        try {
            val resultChannel = Channel<List<DiscoveredService>>(Channel.UNLIMITED)
            
            withTimeout(DISCOVERY_TIMEOUT_MS) {
                startDiscovery(resultChannel)
                
                // Emit initial empty list
                emit(emptyList())
                
                // Emit updates as services are discovered
                for (services in resultChannel) {
                    emit(services)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.i(TAG, "Discovery timeout reached")
            emit(discoveredServices.values.toList())
        } catch (e: Exception) {
            Log.e(TAG, "Discovery failed: ${e.message}", e)
            emit(emptyList())
        } finally {
            stopDiscovery()
        }
    }
    
    /**
     * Discover services synchronously with timeout
     */
    suspend fun discoverServicesSync(timeoutMs: Long = DISCOVERY_TIMEOUT_MS): List<DiscoveredService> {
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(timeoutMs) {
                    val resultChannel = Channel<List<DiscoveredService>>(Channel.UNLIMITED)
                    startDiscovery(resultChannel)
                    
                    // Wait for services to be discovered (shorter delay for faster response)
                    delay(minOf(timeoutMs, 2000L))
                    
                    discoveredServices.values.toList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync discovery failed: ${e.message}", e)
                emptyList()
            } finally {
                stopDiscovery()
            }
        }
    }
    
    private fun startDiscovery(resultChannel: Channel<List<DiscoveredService>>) {
        if (isDiscovering.get()) {
            Log.w(TAG, "Discovery already in progress")
            return
        }

        // Acquire multicast lock for mDNS discovery
        acquireMulticastLock()

        discoveredServices.clear()
        
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
                isDiscovering.set(false)
            }
            
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: $errorCode")
                isDiscovering.set(false)
            }
            
            override fun onDiscoveryStarted(serviceType: String?) {
                Log.i(TAG, "Discovery started for: $serviceType")
                isDiscovering.set(true)
            }
            
            override fun onDiscoveryStopped(serviceType: String?) {
                Log.i(TAG, "Discovery stopped for: $serviceType")
                isDiscovering.set(false)
            }
            
            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { service ->
                    Log.i(TAG, "Service found: ${service.serviceName}")
                    resolveService(service, resultChannel)
                }
            }
            
            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { service ->
                    Log.i(TAG, "Service lost: ${service.serviceName}")
                    discoveredServices.remove(service.serviceName)
                    
                    // Emit updated list
                    resultChannel.trySend(discoveredServices.values.toList())
                }
            }
        }
        
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery: ${e.message}", e)
            isDiscovering.set(false)
        }
    }
    
    private fun resolveService(serviceInfo: NsdServiceInfo, resultChannel: Channel<List<DiscoveredService>>) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}: $errorCode")
            }
            
            override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { service ->
                    Log.i(TAG, "Service resolved: ${service.serviceName}")
                    Log.i(TAG, "  Host: ${service.host}")
                    Log.i(TAG, "  Port: ${service.port}")
                    
                    // Skip attributes parsing for now - focus on basic discovery
                    val properties = emptyMap<String, String>()
                    
                    val discoveredService = DiscoveredService(
                        name = service.serviceName,
                        host = service.host.hostAddress ?: service.host.hostName,
                        port = service.port,
                        properties = properties
                    )
                    
                    discoveredServices[service.serviceName] = discoveredService
                    
                    // Emit updated list
                    resultChannel.trySend(discoveredServices.values.toList())
                    
                    Log.i(TAG, "✅ Added service: ${discoveredService.baseUrl}")
                }
            }
        }
        
        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve service: ${e.message}", e)
        }
    }
    
    private fun stopDiscovery() {
        if (isDiscovering.get() && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop discovery: ${e.message}", e)
            }
        }

        // Release multicast lock
        releaseMulticastLock()

        discoveryListener = null
        isDiscovering.set(false)
    }
    
    /**
     * Get currently discovered services
     */
    fun getDiscoveredServices(): List<DiscoveredService> {
        return discoveredServices.values.toList()
    }
    
    /**
     * Check if discovery is currently running
     */
    fun isDiscovering(): Boolean {
        return isDiscovering.get()
    }

    /**
     * Acquire multicast lock to enable mDNS packet reception
     * This is required on many Android devices to receive multicast packets
     */
    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("EntityMDnsDiscovery")
                multicastLock?.setReferenceCounted(true)
            }

            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
                Log.i(TAG, "✅ Multicast lock acquired for mDNS discovery")
            } else {
                Log.d(TAG, "Multicast lock already held")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to acquire multicast lock: ${e.message}", e)
        }
    }

    /**
     * Release multicast lock to save battery
     */
    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.i(TAG, "🔓 Multicast lock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to release multicast lock: ${e.message}", e)
        }
    }

    /**
     * Clean up resources when done
     */
    fun cleanup() {
        stopDiscovery()
        releaseMulticastLock()
        multicastLock = null
    }
}
