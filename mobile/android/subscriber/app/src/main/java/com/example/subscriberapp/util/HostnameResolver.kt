package com.example.subscriberapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException

object HostnameResolver {
    
    private const val TAG = "HostnameResolver"
    
    /**
     * Resolve restaurant.local to actual IP address
     * This is needed because Android doesn't automatically resolve .local hostnames
     */
    suspend fun resolveRestaurantLocal(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🔍 Attempting to resolve restaurant.local...")
            
            // Method 1: Try standard DNS resolution (might work on some networks)
            try {
                val addresses = InetAddress.getAllByName("restaurant.local")
                if (addresses.isNotEmpty()) {
                    val ip = addresses[0].hostAddress
                    Log.i(TAG, "✅ DNS resolved restaurant.local to: $ip")
                    return@withContext ip
                }
            } catch (e: Exception) {
                Log.d(TAG, "DNS resolution failed: ${e.message}")
            }
            
            // Method 2: Scan local network for the server
            Log.i(TAG, "🔍 Scanning local network for server...")
            val networkIp = getCurrentNetworkIp(context)
            if (networkIp != null) {
                Log.i(TAG, "📍 Current device IP: $networkIp")
                val serverIp = scanForServer(networkIp)
                if (serverIp != null) {
                    Log.i(TAG, "✅ Found server at: $serverIp")
                    return@withContext serverIp
                }
            }
            
            // Method 3: Try known server IPs
            val knownIps = listOf("192.168.31.209", "172.20.10.2")
            for (ip in knownIps) {
                Log.i(TAG, "🔍 Trying known server IP: $ip")
                if (testServerConnection(ip)) {
                    Log.i(TAG, "✅ Known server IP works: $ip")
                    return@withContext ip
                }
            }
            
            Log.w(TAG, "❌ Could not resolve restaurant.local")
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resolving restaurant.local: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Get current device IP address on WiFi network
     */
    private fun getCurrentNetworkIp(context: Context): String? {
        try {
            // Try WiFi manager first
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo != null) {
                val ipInt = wifiInfo.ipAddress
                if (ipInt != 0) {
                    val ip = String.format(
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                    Log.d(TAG, "WiFi IP: $ip")
                    return ip
                }
            }
            
            // Fallback: Try network interfaces
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address.hostAddress?.contains(':') == false) {
                            val ip = address.hostAddress
                            Log.d(TAG, "Network interface IP: $ip")
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network IP: ${e.message}")
        }
        return null
    }
    
    /**
     * Scan local network for server
     */
    private suspend fun scanForServer(deviceIp: String): String? = withContext(Dispatchers.IO) {
        try {
            val networkBase = getNetworkBase(deviceIp)
            Log.i(TAG, "🔍 Scanning network: $networkBase.x")
            
            // Common server IPs to try first
            val priorityIps = listOf(
                "$networkBase.209", // Current known server IP
                "$networkBase.2",   // Alternative server IP
                "$networkBase.1",   // Router
                "$networkBase.100", // Common server IP
                "$networkBase.101",
                "$networkBase.10"
            )
            
            // Test priority IPs first
            for (ip in priorityIps) {
                if (testServerConnection(ip)) {
                    Log.i(TAG, "✅ Found server at priority IP: $ip")
                    return@withContext ip
                }
            }
            
            // Quick scan of common range
            for (i in 20..50) {
                val ip = "$networkBase.$i"
                if (testServerConnection(ip)) {
                    Log.i(TAG, "✅ Found server at: $ip")
                    return@withContext ip
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning network: ${e.message}")
        }
        return@withContext null
    }
    
    /**
     * Get network base (e.g., "192.168.1" from "192.168.1.100")
     */
    private fun getNetworkBase(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size >= 3) {
            "${parts[0]}.${parts[1]}.${parts[2]}"
        } else {
            "192.168.31" // fallback to current network
        }
    }
    
    /**
     * Test if server is running at given IP
     */
    private fun testServerConnection(ip: String): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ip, 8080), 2000) // 2 second timeout
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
