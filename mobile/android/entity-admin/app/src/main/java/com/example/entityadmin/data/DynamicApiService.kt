package com.example.entityadmin.data

import android.content.Context
import android.util.Log
import com.example.entityadmin.data.api.ApiService
import com.example.entityadmin.util.ServerDiscovery
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamic API service that can update its base URL when server discovery finds a new server
 */
@Singleton
class DynamicApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "EntityDynamicApiService"
        private const val PREFS_NAME = "entity_server_discovery"
        private const val KEY_DISCOVERED_SERVER = "discovered_server"
    }
    
    private var currentBaseUrl: String? = null
    private var currentApiService: ApiService? = null
    
    /**
     * Get the API service, creating or updating it if necessary
     */
    fun getApiService(): ApiService {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val discoveredServer = prefs.getString(KEY_DISCOVERED_SERVER, null)
        
        // If we have a new server URL or no current service, create a new one
        if (discoveredServer != null && discoveredServer != currentBaseUrl) {
            Log.i(TAG, "🔄 Updating API service to use server: $discoveredServer")
            currentBaseUrl = discoveredServer
            currentApiService = createApiService(discoveredServer)
        } else if (currentApiService == null) {
            // No discovered server yet, use fallback discovery
            Log.i(TAG, "🔍 No server discovered yet, running discovery...")
            val fallbackServer = ServerDiscovery.discoverServerUrl(context)
            Log.i(TAG, "✅ Using fallback server: $fallbackServer")
            currentBaseUrl = fallbackServer
            currentApiService = createApiService(fallbackServer)
            
            // Save the fallback server
            prefs.edit().putString(KEY_DISCOVERED_SERVER, fallbackServer).apply()
        }
        
        return currentApiService!!
    }
    
    /**
     * Force refresh the API service with a new server URL
     */
    fun refreshWithServer(serverUrl: String) {
        Log.i(TAG, "🔄 Force refreshing API service with server: $serverUrl")
        currentBaseUrl = serverUrl
        currentApiService = createApiService(serverUrl)
        
        // Save the new server
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISCOVERED_SERVER, serverUrl).apply()
    }
    
    /**
     * Get the current base URL being used
     */
    fun getCurrentBaseUrl(): String? = currentBaseUrl
    
    /**
     * Test if the current server is still responding
     */
    fun testCurrentServer(): Boolean {
        return currentBaseUrl?.let { ServerDiscovery.testServerQuick(it) } ?: false
    }
    
    /**
     * Force rediscovery of the server
     */
    fun rediscoverServer(): String {
        Log.i(TAG, "🔍 Force rediscovering server...")
        
        // Clear cached server
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DISCOVERED_SERVER).apply()
        
        // Discover new server
        val newServer = ServerDiscovery.discoverServerUrl(context)
        Log.i(TAG, "✅ Rediscovered server: $newServer")
        
        // Update API service
        refreshWithServer(newServer)
        
        return newServer
    }
    
    private fun createApiService(baseUrl: String): ApiService {
        Log.i(TAG, "🏗️ Creating new API service with base URL: $baseUrl")
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        return retrofit.create(ApiService::class.java)
    }
}
