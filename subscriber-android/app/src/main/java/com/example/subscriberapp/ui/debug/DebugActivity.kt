package com.example.subscriberapp.ui.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subscriberapp.util.ServerDiscovery
import com.example.subscriberapp.util.MDnsDiscovery
import com.example.subscriberapp.util.HostnameResolver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class DebugActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "DebugActivity"
        
        fun start(context: Context) {
            val intent = Intent(context, DebugActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                DebugScreen()
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DebugScreen() {
        var logs by remember { mutableStateOf(listOf<String>()) }
        var isRunning by remember { mutableStateOf(false) }
        var discoveredServer by remember { mutableStateOf<String?>(null) }
        
        val scope = rememberCoroutineScope()
        
        fun addLog(message: String) {
            logs = logs + "[${System.currentTimeMillis() % 100000}] $message"
            Log.d(TAG, message)
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "🔧 Server Discovery Debug",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (discoveredServer != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✅ Server Found!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = discoveredServer!!,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!isRunning) {
                            scope.launch {
                                isRunning = true
                                logs = emptyList()
                                discoveredServer = null
                                runServerDiscovery(
                                    addLog = { addLog(it) },
                                    onServerFound = { server -> discoveredServer = server }
                                )
                                isRunning = false
                            }
                        }
                    },
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Text("🚀 Start Discovery")
                    }
                }
                
                Button(
                    onClick = {
                        logs = emptyList()
                        discoveredServer = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🗑️ Clear Logs")
                }
            }
            
            Card(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                log.contains("✅") -> Color(0xFF4CAF50)
                                log.contains("❌") -> Color(0xFFF44336)
                                log.contains("⚠️") -> Color(0xFFFF9800)
                                log.contains("🔍") -> Color(0xFF2196F3)
                                else -> Color.Black
                            }
                        )
                    }
                }
            }
        }
    }
    
    private suspend fun runServerDiscovery(
        addLog: (String) -> Unit,
        onServerFound: (String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        var foundServer: String? = null

        try {
            addLog("🚀 Starting comprehensive server discovery...")

            // Test current network info
            addLog("📱 Testing network connectivity...")

            // Test direct IP first
            val directIP = "http://192.168.31.209:8080/"
            addLog("🔍 Testing direct IP: $directIP")
            if (testServer(directIP)) {
                addLog("✅ Direct IP works: $directIP")
                foundServer = directIP
            } else {
                addLog("❌ Direct IP failed: $directIP")
            }

            // Test hostname resolution
            addLog("🏠 Attempting to resolve restaurant.local...")
            val resolvedIp = HostnameResolver.resolveRestaurantLocal(this@DebugActivity)
            if (resolvedIp != null) {
                val resolvedUrl = "http://$resolvedIp:8080/"
                addLog("✅ Resolved restaurant.local to: $resolvedIp")
                addLog("🔍 Testing resolved URL: $resolvedUrl")
                if (testServer(resolvedUrl)) {
                    addLog("✅ Resolved hostname works: $resolvedUrl")
                    if (foundServer == null) foundServer = resolvedUrl
                } else {
                    addLog("❌ Resolved hostname failed: $resolvedUrl")
                }
            } else {
                addLog("❌ Could not resolve restaurant.local")
            }

            // Test direct mDNS hostname (fallback)
            val mdnsHostname = "http://restaurant.local:8080/"
            addLog("🏠 Testing direct mDNS hostname: $mdnsHostname")
            if (testServer(mdnsHostname)) {
                addLog("✅ Direct mDNS hostname works: $mdnsHostname")
                if (foundServer == null) foundServer = mdnsHostname
            } else {
                addLog("❌ Direct mDNS hostname failed: $mdnsHostname")
            }

            // Run full discovery
            addLog("🔍 Running full ServerDiscovery...")
            val discoveredUrl = ServerDiscovery.discoverServerUrl(this@DebugActivity)
            addLog("📋 ServerDiscovery result: $discoveredUrl")

            if (foundServer == null) foundServer = discoveredUrl

            withContext(Dispatchers.Main) {
                onServerFound(foundServer)
            }

            addLog("✅ Discovery completed!")

        } catch (e: Exception) {
            addLog("❌ Discovery failed: ${e.message}")
        }
    }
    
    private fun testServer(url: String): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
                
            val request = Request.Builder()
                .url(url + "subscriber/health")
                .build()
                
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            val success = response.isSuccessful && body?.contains("healthy") == true
            response.close()
            success
        } catch (e: Exception) {
            false
        }
    }
}
