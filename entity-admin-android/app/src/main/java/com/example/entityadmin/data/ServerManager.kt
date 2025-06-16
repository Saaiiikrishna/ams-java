package com.example.entityadmin.data

import android.content.Context
import android.util.Log
import com.example.entityadmin.util.ServerDiscovery
import com.example.entityadmin.util.MDnsDiscovery
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages server discovery and updates the API service when a new server is found
 */
@Singleton
class ServerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dynamicApiService: DynamicApiService
) {
    
    companion object {
        private const val TAG = "EntityServerManager"
    }
    
    private val _serverStatus = MutableStateFlow<ServerStatus>(ServerStatus.Discovering)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()
    
    private val _discoveredServer = MutableStateFlow<String?>(null)
    val discoveredServer: StateFlow<String?> = _discoveredServer.asStateFlow()
    
    private var discoveryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    sealed class ServerStatus {
        object Discovering : ServerStatus()
        data class Found(val serverUrl: String) : ServerStatus()
        data class Error(val message: String) : ServerStatus()
    }
    
    init {
        // Start initial discovery
        startServerDiscovery()
    }
    
    /**
     * Start server discovery process
     */
    fun startServerDiscovery() {
        Log.i(TAG, "🚀 Starting entity admin server discovery...")
        
        discoveryJob?.cancel()
        _serverStatus.value = ServerStatus.Discovering
        
        discoveryJob = scope.launch {
            try {
                // First check if we have a cached server that's still working
                val currentServer = dynamicApiService.getCurrentBaseUrl()
                if (currentServer != null && dynamicApiService.testCurrentServer()) {
                    Log.i(TAG, "✅ Current server still working: $currentServer")
                    updateServerFound(currentServer)
                    return@launch
                }
                
                // Try fast async discovery first
                Log.i(TAG, "🔍 Running fast server discovery...")
                val fastServer = ServerDiscovery.discoverServerUrlAsync(context)
                
                if (ServerDiscovery.testServerQuick(fastServer)) {
                    Log.i(TAG, "✅ Fast discovery found working server: $fastServer")
                    updateServerFound(fastServer)
                    return@launch
                }
                
                // If fast discovery didn't work, try mDNS discovery
                Log.i(TAG, "📡 Trying mDNS discovery...")
                val mdnsDiscovery = MDnsDiscovery(context)
                val services = mdnsDiscovery.discoverServicesSync(5000L)
                
                for (service in services) {
                    if (ServerDiscovery.testServerQuick(service.baseUrl)) {
                        Log.i(TAG, "✅ mDNS discovery found working server: ${service.baseUrl}")
                        updateServerFound(service.baseUrl)
                        return@launch
                    }
                }
                
                // Last resort - force rediscovery
                Log.w(TAG, "⚠️ No working server found, forcing rediscovery...")
                val rediscoveredServer = dynamicApiService.rediscoverServer()
                updateServerFound(rediscoveredServer)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Server discovery failed: ${e.message}", e)
                _serverStatus.value = ServerStatus.Error("Discovery failed: ${e.message}")
            }
        }
    }
    
    /**
     * Manually set a server URL
     */
    fun setManualServer(serverUrl: String) {
        Log.i(TAG, "🔧 Manually setting server: $serverUrl")
        
        scope.launch {
            if (ServerDiscovery.testServerQuick(serverUrl)) {
                Log.i(TAG, "✅ Manual server is working: $serverUrl")
                updateServerFound(serverUrl)
            } else {
                Log.e(TAG, "❌ Manual server is not responding: $serverUrl")
                _serverStatus.value = ServerStatus.Error("Manual server not responding")
            }
        }
    }
    
    /**
     * Force refresh server discovery
     */
    fun refreshServerDiscovery() {
        Log.i(TAG, "🔄 Force refreshing server discovery...")
        startServerDiscovery()
    }
    
    /**
     * Get the current server URL
     */
    fun getCurrentServer(): String? {
        return _discoveredServer.value
    }
    
    /**
     * Test if the current server is still working
     */
    suspend fun testCurrentServer(): Boolean {
        return withContext(Dispatchers.IO) {
            dynamicApiService.testCurrentServer()
        }
    }
    
    private fun updateServerFound(serverUrl: String) {
        Log.i(TAG, "✅ Server found and verified: $serverUrl")
        
        // Update the dynamic API service
        dynamicApiService.refreshWithServer(serverUrl)
        
        // Update state
        _discoveredServer.value = serverUrl
        _serverStatus.value = ServerStatus.Found(serverUrl)
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        discoveryJob?.cancel()
        scope.cancel()
    }
}
