package com.example.subscriberapp.util

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

object ServerDiscovery {
    
    private const val TAG = "ServerDiscovery"
    private const val PREFS_NAME = "server_discovery"
    private const val KEY_DISCOVERED_SERVER = "discovered_server"
    private const val HEALTH_ENDPOINT = "subscriber/health"
    
    private val quickClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)  // Increased from 2 seconds
        .readTimeout(15, TimeUnit.SECONDS)     // Increased from 3 seconds
        .writeTimeout(15, TimeUnit.SECONDS)    // Added write timeout
        .callTimeout(20, TimeUnit.SECONDS)     // Added overall call timeout
        .build()

    /**
     * Discover server URL with mDNS first, then fallback options (DEPRECATED - use async version)
     */
    @Deprecated("Use discoverServerUrlAsync for better performance")
    fun discoverServerUrl(context: Context): String {
        Log.i(TAG, "🚀 Starting comprehensive server discovery...")
        Log.i(TAG, "📱 Device info: Android ${android.os.Build.VERSION.RELEASE}")

        // Check network connectivity first
        checkNetworkConnectivity(context)

        // Try to get previously discovered server
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedServer = prefs.getString(KEY_DISCOVERED_SERVER, null)

        if (savedServer != null) {
            Log.i(TAG, "🔍 Testing saved server: $savedServer")
            if (testServerQuick(savedServer)) {
                Log.i(TAG, "✅ Using saved server: $savedServer")
                return savedServer
            } else {
                Log.w(TAG, "❌ Saved server not responding, clearing cache")
                prefs.edit().remove(KEY_DISCOVERED_SERVER).apply()
            }
        }

        Log.i(TAG, "🌐 Starting fresh network discovery for backend server...")

        // First try to resolve mDNS hostname
        Log.i(TAG, "🏠 Attempting to resolve restaurant.local...")
        val resolvedIp = runBlocking {
            HostnameResolver.resolveRestaurantLocal(context)
        }

        if (resolvedIp != null) {
            val resolvedUrl = "http://$resolvedIp:8080/"
            Log.i(TAG, "🔍 Testing resolved IP: $resolvedUrl")
            if (testServerQuick(resolvedUrl)) {
                Log.i(TAG, "✅ SUCCESS! Discovered server via hostname resolution: $resolvedUrl")
                prefs.edit().putString(KEY_DISCOVERED_SERVER, resolvedUrl).apply()
                return resolvedUrl
            }
        }

        // Also try direct hostname (might work on some networks)
        val mdnsHostname = "http://restaurant.local:8080/"
        Log.i(TAG, "🏠 Testing direct mDNS hostname: $mdnsHostname")
        if (testServerQuick(mdnsHostname)) {
            Log.i(TAG, "✅ SUCCESS! Discovered server via direct mDNS hostname: $mdnsHostname")
            prefs.edit().putString(KEY_DISCOVERED_SERVER, mdnsHostname).apply()
            return mdnsHostname
        } else {
            Log.w(TAG, "❌ mDNS hostname resolution failed")
        }

        // Then try mDNS service discovery
        Log.i(TAG, "📡 Attempting mDNS service discovery...")
        val mdnsDiscoveredServer = tryMDnsDiscovery(context)
        if (mdnsDiscoveredServer != null) {
            Log.i(TAG, "✅ SUCCESS! Discovered server via mDNS service discovery: $mdnsDiscoveredServer")
            prefs.edit().putString(KEY_DISCOVERED_SERVER, mdnsDiscoveredServer).apply()
            return mdnsDiscoveredServer
        } else {
            Log.w(TAG, "❌ mDNS service discovery failed")
        }

        // Fallback to IP scanning
        Log.i(TAG, "🔍 mDNS discovery failed, falling back to comprehensive IP scanning...")

        // List of servers to try in order of preference
        val serversToTry = mutableListOf<String>()

        // Add network-based servers first (most likely to work)
        val networkServers = getNetworkServers()
        Log.i(TAG, "📍 Found ${networkServers.size} network-based servers to try")
        serversToTry.addAll(networkServers)

        // Add standard fallbacks
        val fallbackServers = listOf(
            "http://192.168.31.209:8080/", // Current known server IP
            "http://172.20.10.2:8080/", // Alternative network server IP
            "http://localhost:8080/",
            "http://10.0.2.2:8080/", // Android emulator
            "http://127.0.0.1:8080/",
            "http://192.168.1.1:8080/",
            "http://192.168.0.1:8080/",
            "http://192.168.31.1:8080/",
            "http://172.20.10.1:8080/", // Alternative network gateway
            "http://10.0.0.1:8080/"
        )

        Log.i(TAG, "📋 Adding ${fallbackServers.size} fallback servers")
        serversToTry.addAll(fallbackServers)

        val uniqueServers = serversToTry.distinct()
        Log.i(TAG, "🎯 Testing ${uniqueServers.size} potential servers...")

        // Test each server quickly
        for ((index, serverUrl) in uniqueServers.withIndex()) {
            Log.i(TAG, "🔍 [${index + 1}/${uniqueServers.size}] Testing server: $serverUrl")
            if (testServerQuick(serverUrl)) {
                Log.i(TAG, "✅ SUCCESS! Discovered working server: $serverUrl")
                // Save the working server
                prefs.edit().putString(KEY_DISCOVERED_SERVER, serverUrl).apply()
                return serverUrl
            } else {
                Log.d(TAG, "❌ Server not responding: $serverUrl")
            }
        }

        // If nothing works, return the mDNS discovered server IP as last resort
        val lastResort = "http://192.168.31.4:8080/" // This is what mDNS discovered
        Log.w(TAG, "⚠️ No server discovered via scanning, using mDNS discovered server: $lastResort")
        return lastResort
    }

    /**
     * Fast async server discovery with optimized performance
     */
    suspend fun discoverServerUrlAsync(context: Context): String = withContext(Dispatchers.IO) {
        Log.i(TAG, "🚀 Starting fast async server discovery...")

        // Check network connectivity first
        checkNetworkConnectivity(context)

        // Try to get previously discovered server first (fastest option)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedServer = prefs.getString(KEY_DISCOVERED_SERVER, null)

        if (savedServer != null) {
            Log.i(TAG, "🔍 Testing saved server: $savedServer")
            if (testServerHealth(savedServer)) {
                Log.i(TAG, "✅ Using cached server: $savedServer")
                return@withContext savedServer
            } else {
                Log.w(TAG, "❌ Cached server not responding, clearing cache")
                prefs.edit().remove(KEY_DISCOVERED_SERVER).apply()
            }
        }

        // Try direct hostname resolution first (fast)
        Log.i(TAG, "🏠 Testing direct mDNS hostname...")
        val mdnsHostname = "http://restaurant.local:8080/"
        if (testServerHealth(mdnsHostname)) {
            Log.i(TAG, "✅ SUCCESS! Direct mDNS hostname works: $mdnsHostname")
            prefs.edit().putString(KEY_DISCOVERED_SERVER, mdnsHostname).apply()
            return@withContext mdnsHostname
        }

        // Try hostname resolution
        try {
            val resolvedIp = HostnameResolver.resolveRestaurantLocal(context)
            if (resolvedIp != null) {
                val resolvedUrl = "http://$resolvedIp:8080/"
                Log.i(TAG, "🔍 Testing resolved IP: $resolvedUrl")
                if (testServerHealth(resolvedUrl)) {
                    Log.i(TAG, "✅ SUCCESS! Resolved hostname works: $resolvedUrl")
                    prefs.edit().putString(KEY_DISCOVERED_SERVER, resolvedUrl).apply()
                    return@withContext resolvedUrl
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hostname resolution failed: ${e.message}")
        }

        // Try fast mDNS discovery (limited time)
        Log.i(TAG, "📡 Attempting fast mDNS discovery...")
        try {
            val mdnsDiscovery = MDnsDiscovery(context)
            val services = mdnsDiscovery.discoverServicesSync(3000L) // Only 3 seconds for fast discovery

            for (service in services) {
                if (testServerHealth(service.baseUrl)) {
                    Log.i(TAG, "✅ SUCCESS! mDNS service works: ${service.baseUrl}")
                    prefs.edit().putString(KEY_DISCOVERED_SERVER, service.baseUrl).apply()
                    return@withContext service.baseUrl
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fast mDNS discovery failed: ${e.message}")
        }

        // Fast fallback to known working servers
        val quickFallbacks = listOf(
            "http://192.168.31.4:8080/", // Current known server (from mDNS discovery)
            "http://192.168.31.209:8080/", // Alternative
            "http://10.0.2.2:8080/", // Emulator
            "http://localhost:8080/"
        )

        Log.i(TAG, "🔍 Testing quick fallback servers...")
        for (serverUrl in quickFallbacks) {
            if (testServerHealth(serverUrl)) {
                Log.i(TAG, "✅ SUCCESS! Fallback server works: $serverUrl")
                prefs.edit().putString(KEY_DISCOVERED_SERVER, serverUrl).apply()
                return@withContext serverUrl
            }
        }

        // Last resort - use the server that was discovered by mDNS if available
        val lastResort = "http://192.168.31.4:8080/" // This is the server that mDNS found
        Log.w(TAG, "⚠️ Using last resort server (mDNS discovered): $lastResort")
        return@withContext lastResort
    }

    /**
     * Check network connectivity and log details
     */
    private fun checkNetworkConnectivity(context: Context) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

            if (network != null && networkCapabilities != null) {
                Log.i(TAG, "📶 Network connectivity:")
                Log.i(TAG, "   Connected: ✅")
                Log.i(TAG, "   WiFi: ${if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) "✅" else "❌"}")
                Log.i(TAG, "   Cellular: ${if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) "✅" else "❌"}")
                Log.i(TAG, "   Internet: ${if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) "✅" else "❌"}")
                Log.i(TAG, "   Validated: ${if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "✅" else "❌"}")
            } else {
                Log.w(TAG, "❌ No active network connection")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking network connectivity: ${e.message}")
        }
    }

    /**
     * Get potential servers based on current network
     */
    private fun getNetworkServers(): List<String> {
        val servers = mutableListOf<String>()

        try {
            val currentIp = getCurrentIpAddress()
            if (currentIp != null) {
                Log.d(TAG, "Current IP: $currentIp")
                val networkBase = getNetworkBase(currentIp)

                // Add gateway and common server IPs
                servers.add("http://$networkBase.1:8080/") // Gateway
                servers.add("http://$networkBase.100:8080/") // Common server IP
                servers.add("http://$networkBase.101:8080/")
                servers.add("http://$networkBase.10:8080/")
                servers.add("http://$networkBase.2:8080/")
                servers.add("http://$networkBase.3:8080/")
                servers.add("http://$networkBase.4:8080/")
                servers.add("http://$networkBase.5:8080/")

                // Scan common IP ranges for the network
                for (i in 20..50) {
                    servers.add("http://$networkBase.$i:8080/")
                }

                // Add the current device's IP (in case it's running the server)
                servers.add("http://$currentIp:8080/")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network servers", e)
        }

        return servers
    }

    /**
     * Get current device IP address
     */
    private fun getCurrentIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress?.contains(':') == false) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current IP", e)
        }
        return null
    }

    /**
     * Get network base (e.g., "192.168.1" from "192.168.1.100")
     */
    private fun getNetworkBase(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size >= 3) {
            "${parts[0]}.${parts[1]}.${parts[2]}"
        } else {
            "192.168.1" // fallback
        }
    }

    /**
     * Quick test if server is responding
     */
    fun testServerQuick(baseUrl: String): Boolean {
        return try {
            val url = if (baseUrl.endsWith("/")) {
                baseUrl + HEALTH_ENDPOINT
            } else {
                "$baseUrl/$HEALTH_ENDPOINT"
            }

            Log.d(TAG, "🔍 Testing URL: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "SubscriberApp/1.0")
                .build()

            val response = quickClient.newCall(request).execute()
            val responseBody = response.body?.string()
            val isHealthy = response.isSuccessful && responseBody?.contains("healthy") == true

            Log.d(TAG, "📊 Response for $baseUrl:")
            Log.d(TAG, "   Status: ${response.code}")
            Log.d(TAG, "   Success: ${response.isSuccessful}")
            Log.d(TAG, "   Body: ${responseBody?.take(100)}")
            Log.d(TAG, "   Result: ${if (isHealthy) "✅ PASS" else "❌ FAIL"}")

            response.close()
            isHealthy
        } catch (e: Exception) {
            Log.w(TAG, "❌ Quick test failed for $baseUrl: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    /**
     * Test server health asynchronously
     */
    suspend fun testServerHealth(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = if (baseUrl.endsWith("/")) {
                baseUrl + HEALTH_ENDPOINT
            } else {
                "$baseUrl/$HEALTH_ENDPOINT"
            }
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = quickClient.newCall(request).execute()
            val responseBody = response.body?.string()
            val isHealthy = response.isSuccessful && responseBody?.contains("healthy") == true
            
            Log.d(TAG, "Health test for $baseUrl: ${if (isHealthy) "PASS" else "FAIL"}")
            response.close()
            return@withContext isHealthy
        } catch (e: Exception) {
            Log.d(TAG, "Health test failed for $baseUrl: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Clear saved server (force rediscovery)
     */
    fun clearSavedServer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DISCOVERED_SERVER).apply()
        Log.d(TAG, "Cleared saved server")
    }

    /**
     * Get currently saved server
     */
    fun getSavedServer(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISCOVERED_SERVER, null)
    }

    /**
     * Manually set server URL
     */
    fun setManualServer(context: Context, serverUrl: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISCOVERED_SERVER, serverUrl).apply()
        Log.d(TAG, "Manually set server: $serverUrl")
    }

    /**
     * Try mDNS discovery first
     */
    private fun tryMDnsDiscovery(context: Context): String? {
        return try {
            Log.i(TAG, "📡 Attempting mDNS service discovery...")
            Log.i(TAG, "🔍 Looking for service type: _attendanceapi._tcp")

            val mdnsDiscovery = MDnsDiscovery(context)
            val discoveredServices = runBlocking {
                Log.i(TAG, "⏱️ Starting 8-second mDNS scan...")
                mdnsDiscovery.discoverServicesSync(8000L) // 8 second timeout for better discovery
            }

            Log.i(TAG, "📋 mDNS discovery completed. Found ${discoveredServices.size} services")

            if (discoveredServices.isNotEmpty()) {
                // Test each discovered service
                for ((index, service) in discoveredServices.withIndex()) {
                    Log.i(TAG, "🔍 [${index + 1}/${discoveredServices.size}] Testing mDNS service:")
                    Log.i(TAG, "   Name: ${service.name}")
                    Log.i(TAG, "   Host: ${service.host}")
                    Log.i(TAG, "   Port: ${service.port}")
                    Log.i(TAG, "   URL: ${service.baseUrl}")

                    if (testServerQuick(service.baseUrl)) {
                        Log.i(TAG, "✅ SUCCESS! mDNS service verified: ${service.baseUrl}")
                        return service.baseUrl
                    } else {
                        Log.w(TAG, "❌ mDNS service not responding: ${service.baseUrl}")
                    }
                }
                Log.w(TAG, "❌ None of the ${discoveredServices.size} mDNS services responded")
            } else {
                Log.w(TAG, "❌ No mDNS services discovered")
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ mDNS discovery failed: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    /**
     * Discover services asynchronously with callback
     */
    suspend fun discoverServerAsync(context: Context, callback: (String?) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Starting async mDNS discovery...")

                val mdnsDiscovery = MDnsDiscovery(context)
                val services = mdnsDiscovery.discoverServicesSync(8000L)

                for (service in services) {
                    if (testServerHealth(service.baseUrl)) {
                        Log.i(TAG, "✅ Async discovered server: ${service.baseUrl}")

                        // Save the discovered server
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_DISCOVERED_SERVER, service.baseUrl).apply()

                        withContext(Dispatchers.Main) {
                            callback(service.baseUrl)
                        }
                        return@withContext
                    }
                }

                // No mDNS services found, try fallback
                val fallbackServer = discoverServerUrl(context)
                withContext(Dispatchers.Main) {
                    callback(fallbackServer)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Async discovery failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }
}
