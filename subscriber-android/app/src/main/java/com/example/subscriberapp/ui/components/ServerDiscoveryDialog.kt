package com.example.subscriberapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.subscriberapp.util.MDnsDiscovery
import com.example.subscriberapp.util.ServerDiscovery
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDiscoveryDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onServerSelected: (String) -> Unit
) {
    if (!isVisible) return
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var discoveryState by remember { mutableStateOf(DiscoveryState.SEARCHING) }
    var discoveredServices by remember { mutableStateOf<List<MDnsDiscovery.DiscoveredService>>(emptyList()) }
    var selectedServer by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Start discovery when dialog opens
    LaunchedEffect(isVisible) {
        if (isVisible) {
            discoveryState = DiscoveryState.SEARCHING
            errorMessage = null
            
            try {
                val mdnsDiscovery = MDnsDiscovery(context)
                
                // Collect discovery results
                mdnsDiscovery.discoverServices().collect { services ->
                    discoveredServices = services
                    
                    if (services.isNotEmpty()) {
                        discoveryState = DiscoveryState.FOUND
                    }
                }
                
                // If no services found after timeout, show fallback options
                delay(8000)
                if (discoveredServices.isEmpty()) {
                    discoveryState = DiscoveryState.FALLBACK
                }
                
            } catch (e: Exception) {
                errorMessage = e.message
                discoveryState = DiscoveryState.ERROR
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Server Discovery",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (discoveryState) {
                    DiscoveryState.SEARCHING -> {
                        SearchingContent()
                    }
                    DiscoveryState.FOUND -> {
                        FoundServicesContent(
                            services = discoveredServices,
                            selectedServer = selectedServer,
                            onServerSelected = { selectedServer = it }
                        )
                    }
                    DiscoveryState.FALLBACK -> {
                        FallbackContent(
                            onManualEntry = { server ->
                                selectedServer = server
                                discoveryState = DiscoveryState.FOUND
                            }
                        )
                    }
                    DiscoveryState.ERROR -> {
                        ErrorContent(errorMessage = errorMessage)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    if (selectedServer != null) {
                        Button(
                            onClick = {
                                selectedServer?.let { server ->
                                    onServerSelected(server)
                                    onDismiss()
                                }
                            }
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Searching",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CircularProgressIndicator()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Searching for servers on your network...",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Make sure you're connected to the same WiFi network as the server.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FoundServicesContent(
    services: List<MDnsDiscovery.DiscoveredService>,
    selectedServer: String?,
    onServerSelected: (String) -> Unit
) {
    Column {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Found",
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterHorizontally),
            tint = Color.Green
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Found ${services.size} server(s)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(services) { service ->
                ServiceItem(
                    service = service,
                    isSelected = selectedServer == service.baseUrl,
                    onSelected = { onServerSelected(service.baseUrl) }
                )
            }
        }
    }
}

@Composable
private fun ServiceItem(
    service: MDnsDiscovery.DiscoveredService,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        onClick = onSelected
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${service.host}:${service.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FallbackContent(
    onManualEntry: (String) -> Unit
) {
    var manualUrl by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "No servers found",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No servers found automatically",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You can enter the server address manually:",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = manualUrl,
            onValueChange = { manualUrl = it },
            label = { Text("Server URL") },
            placeholder = { Text("http://192.168.1.100:8080") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { 
                if (manualUrl.isNotBlank()) {
                    val url = if (!manualUrl.startsWith("http")) {
                        "http://$manualUrl"
                    } else manualUrl
                    
                    val finalUrl = if (!url.endsWith("/")) "$url/" else url
                    onManualEntry(finalUrl)
                }
            },
            enabled = manualUrl.isNotBlank()
        ) {
            Text("Use This Server")
        }
    }
}

@Composable
private fun ErrorContent(errorMessage: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Discovery Failed",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private enum class DiscoveryState {
    SEARCHING,
    FOUND,
    FALLBACK,
    ERROR
}
