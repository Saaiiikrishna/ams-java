package com.example.entityadmin.ui.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.entityadmin.data.ServerManager
import com.example.entityadmin.ui.theme.EntityAdminTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DebugActivity : ComponentActivity() {

    @Inject
    lateinit var serverManager: ServerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            EntityAdminTheme {
                DebugScreen(serverManager = serverManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(serverManager: ServerManager) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(listOf<String>()) }
    
    // Observe server manager state
    val serverStatus by serverManager.serverStatus.collectAsState()
    val discoveredServer by serverManager.discoveredServer.collectAsState()

    fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        logs = logs + "[$timestamp] $message"
    }

    // Add logs based on server status changes
    LaunchedEffect(serverStatus) {
        when (val currentStatus = serverStatus) {
            is ServerManager.ServerStatus.Discovering -> {
                addLog("🔍 Server discovery in progress...")
            }
            is ServerManager.ServerStatus.Found -> {
                addLog("✅ Server found: ${currentStatus.serverUrl}")
            }
            is ServerManager.ServerStatus.Error -> {
                addLog("❌ Server discovery error: ${currentStatus.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        addLog("🚀 Entity Admin Debug Activity started")
        addLog("📱 Device: Android ${android.os.Build.VERSION.RELEASE}")
        addLog("🔍 Starting server discovery...")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Entity Admin Debug",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Server Status
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Server Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                when (val currentStatus = serverStatus) {
                    is ServerManager.ServerStatus.Discovering -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔍 Discovering...")
                        }
                    }
                    is ServerManager.ServerStatus.Found -> {
                        Text("✅ Found: ${currentStatus.serverUrl}", color = MaterialTheme.colorScheme.primary)
                    }
                    is ServerManager.ServerStatus.Error -> {
                        Text("❌ Error: ${currentStatus.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                
                discoveredServer?.let { server ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Current: $server", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    addLog("🔄 Refreshing server discovery...")
                    serverManager.refreshServerDiscovery()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("🔄 Refresh")
            }
            
            Button(
                onClick = {
                    logs = emptyList()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("🗑️ Clear Logs")
            }
        }
        
        // Manual server entry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    addLog("🔧 Setting manual server: http://192.168.31.4:8080/")
                    serverManager.setManualServer("http://192.168.31.4:8080/")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Use .4:8080")
            }
            
            Button(
                onClick = {
                    addLog("🔧 Setting manual server: http://192.168.31.209:8080/")
                    serverManager.setManualServer("http://192.168.31.209:8080/")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Use .209:8080")
            }
        }

        // Back to Main Button
        Button(
            onClick = {
                val intent = Intent(context, com.example.entityadmin.MainActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🏠 Back to Main App")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logs
        Text(
            text = "Debug Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp),
                reverseLayout = true // Show newest logs at top
            ) {
                items(logs.reversed()) { log ->
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
