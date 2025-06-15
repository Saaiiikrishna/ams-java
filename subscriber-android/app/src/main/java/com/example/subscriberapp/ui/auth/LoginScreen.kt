package com.example.subscriberapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.subscriberapp.util.ServerDiscovery
import com.example.subscriberapp.ui.debug.DebugActivity
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    var mobileNumber by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }
    var discoveryMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by authViewModel.uiState.collectAsState()

    // Location permissions for network discovery
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo/Title
        Text(
            text = "Admin App",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Subscriber Check-in",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Use your mobile number and PIN to login.\nDefault PIN is 0000 for new accounts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Mobile Number Field
        OutlinedTextField(
            value = mobileNumber,
            onValueChange = { mobileNumber = it },
            label = { Text("Mobile Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        // PIN Field
        OutlinedTextField(
            value = pin,
            onValueChange = { newPin ->
                // Limit to 4 digits
                if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
                    pin = newPin
                }
            },
            label = { Text("4-Digit PIN") },
            placeholder = { Text("0000") },
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            trailingIcon = {
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(
                        imageVector = if (showPin) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (showPin) "Hide PIN" else "Show PIN"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Server Discovery Button
        OutlinedButton(
            onClick = {
                // Request location permissions first
                if (!locationPermissions.allPermissionsGranted) {
                    locationPermissions.launchMultiplePermissionRequest()
                } else {
                    scope.launch {
                        isDiscovering = true
                        discoveryMessage = "Searching for server on local network..."

                        // Clear saved server to force rediscovery
                        ServerDiscovery.clearSavedServer(context)

                        // Trigger discovery
                        val discoveredServer = ServerDiscovery.discoverServerUrl(context)

                        isDiscovering = false
                        discoveryMessage = if (discoveredServer.contains("10.0.2.2")) {
                            "No server found on network. Using emulator default."
                        } else {
                            "Server found: $discoveredServer"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDiscovering && !uiState.isLoading
        ) {
            if (isDiscovering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Discover Server"
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isDiscovering) "Searching..." else "Find Server")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Debug Button
        OutlinedButton(
            onClick = {
                DebugActivity.start(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔧 Debug Server Discovery")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login Button
        Button(
            onClick = {
                authViewModel.loginWithPin(mobileNumber, pin)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = mobileNumber.isNotBlank() && pin.length == 4 && !uiState.isLoading && !isDiscovering
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        // Discovery Message
        discoveryMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Error Message
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Success Message
        uiState.successMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
