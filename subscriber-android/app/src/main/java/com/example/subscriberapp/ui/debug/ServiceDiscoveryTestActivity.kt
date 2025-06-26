package com.example.subscriberapp.ui.debug

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.subscriberapp.data.ServerManager
import com.example.subscriberapp.util.EnhancedMDnsDiscovery
import com.example.subscriberapp.util.MDnsDiscovery
import com.example.subscriberapp.util.ServerDiscovery
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug activity to test mDNS service discovery
 * This helps verify that the Android app can discover the backend server
 */
@AndroidEntryPoint
class ServiceDiscoveryTestActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "ServiceDiscoveryTest"
    }
    
    @Inject
    lateinit var serverManager: ServerManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ServiceDiscoveryTestScreen()
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ServiceDiscoveryTestScreen() {
        var discoveryStatus by remember { mutableStateOf("Ready to start discovery") }
        var nsdServices by remember { mutableStateOf<List<MDnsDiscovery.DiscoveredService>>(emptyList()) }
        var jmdnsServices by remember { mutableStateOf<List<EnhancedMDnsDiscovery.DiscoveredService>>(emptyList()) }
        var serverManagerStatus by remember { mutableStateOf("Not started") }
        var isDiscovering by remember { mutableStateOf(false) }
        
        // Observe server manager status
        LaunchedEffect(Unit) {
            serverManager.serverStatus.collectLatest { status ->
                serverManagerStatus = when (status) {
                    is ServerManager.ServerStatus.Discovering -> "🔍 Discovering..."
                    is ServerManager.ServerStatus.Found -> "✅ Found: ${status.serverUrl}"
                    is ServerManager.ServerStatus.Error -> "❌ Error: ${status.message}"
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🔍 mDNS Service Discovery Test",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Server Manager Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Server Manager Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = serverManagerStatus)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { serverManager.startServerDiscovery() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Discovery")
                        }
                        
                        Button(
                            onClick = { serverManager.refreshServerDiscovery() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Refresh")
                        }
                    }
                }
            }
            
            // Manual Discovery Tests
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Manual Discovery Tests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = discoveryStatus)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { testAndroidNSD() },
                            enabled = !isDiscovering,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Android NSD")
                        }
                        
                        Button(
                            onClick = { testJmDNS() },
                            enabled = !isDiscovering,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test JmDNS")
                        }
                    }
                    
                    Button(
                        onClick = { testFallbackDiscovery() },
                        enabled = !isDiscovering,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Fallback Discovery")
                    }
                }
            }
            
            // Results
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (nsdServices.isNotEmpty()) {
                    item {
                        Text(
                            text = "📱 Android NSD Results (${nsdServices.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }
                    items(nsdServices) { service ->
                        ServiceCard(
                            name = service.name,
                            url = service.baseUrl,
                            details = "Host: ${service.host}, Port: ${service.port}"
                        )
                    }
                }
                
                if (jmdnsServices.isNotEmpty()) {
                    item {
                        Text(
                            text = "🔍 JmDNS Results (${jmdnsServices.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green
                        )
                    }
                    items(jmdnsServices) { service ->
                        ServiceCard(
                            name = service.name,
                            url = service.baseUrl,
                            details = "Host: ${service.host}, gRPC: ${service.grpcUrl}"
                        )
                    }
                }
            }
        }
        
        fun testAndroidNSD() {
            isDiscovering = true
            discoveryStatus = "🔍 Testing Android NSD..."
            
            lifecycleScope.launch {
                try {
                    val mdnsDiscovery = MDnsDiscovery(this@ServiceDiscoveryTestActivity)
                    mdnsDiscovery.discoverServices().collect { services ->
                        nsdServices = services
                        discoveryStatus = "📱 Android NSD found ${services.size} services"
                        Log.i(TAG, "Android NSD found ${services.size} services")
                    }
                } catch (e: Exception) {
                    discoveryStatus = "❌ Android NSD failed: ${e.message}"
                    Log.e(TAG, "Android NSD failed", e)
                } finally {
                    isDiscovering = false
                }
            }
        }
        
        fun testJmDNS() {
            isDiscovering = true
            discoveryStatus = "🔍 Testing JmDNS..."
            
            lifecycleScope.launch {
                try {
                    val enhancedDiscovery = EnhancedMDnsDiscovery(this@ServiceDiscoveryTestActivity)
                    enhancedDiscovery.discoverServices().collect { services ->
                        jmdnsServices = services
                        discoveryStatus = "🔍 JmDNS found ${services.size} services"
                        Log.i(TAG, "JmDNS found ${services.size} services")
                    }
                } catch (e: Exception) {
                    discoveryStatus = "❌ JmDNS failed: ${e.message}"
                    Log.e(TAG, "JmDNS failed", e)
                } finally {
                    isDiscovering = false
                }
            }
        }
        
        fun testFallbackDiscovery() {
            isDiscovering = true
            discoveryStatus = "🔍 Testing fallback discovery..."
            
            lifecycleScope.launch {
                try {
                    val fallbackUrl = ServerDiscovery.discoverServerUrl(this@ServiceDiscoveryTestActivity)
                    discoveryStatus = "🎯 Fallback found: $fallbackUrl"
                    Log.i(TAG, "Fallback discovery found: $fallbackUrl")
                } catch (e: Exception) {
                    discoveryStatus = "❌ Fallback failed: ${e.message}"
                    Log.e(TAG, "Fallback discovery failed", e)
                } finally {
                    isDiscovering = false
                }
            }
        }
    }
    
    @Composable
    fun ServiceCard(name: String, url: String, details: String) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Blue
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
