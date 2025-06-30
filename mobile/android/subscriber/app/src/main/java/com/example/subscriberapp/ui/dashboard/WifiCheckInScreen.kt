package com.example.subscriberapp.ui.dashboard

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.subscriberapp.data.model.Subscriber
import com.example.subscriberapp.data.model.Organization
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun WifiCheckInScreen(
    currentUser: Subscriber?,
    currentOrganization: Organization?,
    dashboardViewModel: DashboardViewModel,
    selectedSessionId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToResult: ((Boolean, String, String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()

    // Permission states
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // WiFi network state
    var currentWifiNetwork by remember { mutableStateOf<String?>(null) }
    var isConnectedToWifi by remember { mutableStateOf(false) }
    var isServerReachable by remember { mutableStateOf(false) }
    var isCheckingConnectivity by remember { mutableStateOf(false) }
    var connectivityError by remember { mutableStateOf<String?>(null) }
    var networkSignalStrength by remember { mutableStateOf<Int?>(null) }

    // Session selection state
    var selectedSession by remember { mutableStateOf<ActiveSession?>(null) }
    var showSessionSelector by remember { mutableStateOf(false) }

    // Auto-select session if sessionId is provided
    LaunchedEffect(selectedSessionId, dashboardState.activeSessions) {
        if (selectedSessionId != null && selectedSession == null) {
            selectedSession = dashboardState.activeSessions.find { it.id == selectedSessionId }
        }
    }

    // Determine if user is checked in to the selected session
    val isCheckedInToSelectedSession = selectedSession?.let { session ->
        dashboardState.currentCheckInStatus?.sessionId == session.id &&
        dashboardState.currentCheckInStatus?.isCheckedIn == true
    } ?: false

    // Animation states for WiFi icon
    val wifiIconScale by animateFloatAsState(
        targetValue = if (isConnectedToWifi && isServerReachable) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Check WiFi status and server connectivity
    LaunchedEffect(locationPermissionState.status.isGranted) {
        while (true) {
            if (locationPermissionState.status.isGranted) {
                val wifiInfo = getEnhancedWifiNetworkInfo(context)
                currentWifiNetwork = wifiInfo.first
                isConnectedToWifi = wifiInfo.second
                networkSignalStrength = wifiInfo.third

                if (isConnectedToWifi && currentWifiNetwork != null) {
                    // Check server connectivity
                    isCheckingConnectivity = true
                    connectivityError = null
                    try {
                        val isReachable = dashboardViewModel.checkServerConnectivity()
                        isServerReachable = isReachable
                        if (!isReachable) {
                            connectivityError = "Server not reachable on this network"
                        }
                    } catch (e: Exception) {
                        isServerReachable = false
                        connectivityError = "Network connectivity check failed: ${e.message}"
                    } finally {
                        isCheckingConnectivity = false
                    }
                } else {
                    isServerReachable = false
                    connectivityError = if (!locationPermissionState.status.isGranted) {
                        "Location permission required for Wi-Fi network detection"
                    } else null
                }
            } else {
                currentWifiNetwork = null
                isConnectedToWifi = false
                isServerReachable = false
                connectivityError = "Location permission required for Wi-Fi network detection"
            }

            kotlinx.coroutines.delay(3000) // Check every 3 seconds for better responsiveness
        }
    }

    // Handle success/error messages and navigate to result screen
    LaunchedEffect(dashboardState.successMessage, dashboardState.errorMessage) {
        if (dashboardState.successMessage != null) {
            onNavigateToResult?.invoke(
                true,
                "WiFi Check-In",
                dashboardState.successMessage!!,
                "WiFi"
            )
        } else if (dashboardState.errorMessage != null) {
            onNavigateToResult?.invoke(
                false,
                "WiFi Check-In",
                dashboardState.errorMessage!!,
                "WiFi"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "WiFi Check-In",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Dynamic WiFi Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = when {
                                    isConnectedToWifi && isServerReachable -> listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                    isConnectedToWifi && !isServerReachable -> listOf(
                                        Color(0xFFFF9800), // Orange for WiFi connected but server not reachable
                                        Color(0xFFFF5722)
                                    )
                                    else -> listOf(
                                        MaterialTheme.colorScheme.errorContainer,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Animated WiFi Icon with connectivity indicator
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isConnectedToWifi && isServerReachable -> Icons.Default.Wifi
                                    isConnectedToWifi && !isServerReachable -> Icons.Default.WifiOff
                                    else -> Icons.Default.WifiOff
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer {
                                        scaleX = wifiIconScale
                                        scaleY = wifiIconScale
                                    },
                                tint = Color.White
                            )

                            // Connectivity status indicator
                            if (isCheckingConnectivity) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = when {
                                isConnectedToWifi && isServerReachable -> "Connected & Ready"
                                isConnectedToWifi && !isServerReachable -> "Connected to WiFi"
                                else -> "Not Connected to WiFi"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currentWifiNetwork ?: "No network detected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )

                        // Network signal strength indicator
                        if (isConnectedToWifi && networkSignalStrength != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = getSignalIcon(networkSignalStrength!!),
                                    contentDescription = "Signal Strength",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = getSignalStrengthText(networkSignalStrength!!),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Server connectivity status
                        if (isConnectedToWifi && !isServerReachable && !isCheckingConnectivity) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = connectivityError ?: "Server not reachable",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Permission status
                        if (!locationPermissionState.status.isGranted) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Location permission required",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Permission Request Card
            if (!locationPermissionState.status.isGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Location permission is required to detect Wi-Fi networks on Android. This helps identify which network you're connected to for attendance verification.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { locationPermissionState.launchPermissionRequest() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            // Warning Card (when WiFi connected but server not reachable)
            if (isConnectedToWifi && !isServerReachable && !isCheckingConnectivity && locationPermissionState.status.isGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Network Issue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "You are connected to WiFi but the attendance server is not reachable on this network. Please connect to your organization's authorized WiFi network to check in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Session Selection Button (only visible when both WiFi and server are connected and permission granted)
            AnimatedVisibility(
                visible = isConnectedToWifi && isServerReachable && currentWifiNetwork != null && locationPermissionState.status.isGranted,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Selected session display
                    selectedSession?.let { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Selected Session",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = session.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    session.description?.let { desc ->
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                // Change selection button
                                TextButton(
                                    onClick = {
                                        selectedSession = null
                                        showSessionSelector = true
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Change",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Session selection or check-in button
                    if (selectedSession == null) {
                        Button(
                            onClick = { showSessionSelector = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Select Session to Check In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                currentUser?.let { user ->
                                    currentOrganization?.let { org ->
                                        currentWifiNetwork?.let { network ->
                                            val action = if (isCheckedInToSelectedSession) "check-out" else "check-in"
                                            android.util.Log.d("WifiCheckIn", "Attempting WiFi $action with session: ${selectedSession!!.id}, network: $network")
                                            dashboardViewModel.performWifiCheckInForSession(
                                                user.mobileNumber,
                                                org.entityId,
                                                network,
                                                selectedSession!!.id
                                            )
                                        } ?: run {
                                            android.util.Log.e("WifiCheckIn", "No WiFi network detected")
                                        }
                                    } ?: run {
                                        android.util.Log.e("WifiCheckIn", "No organization found")
                                    }
                                } ?: run {
                                    android.util.Log.e("WifiCheckIn", "No user found")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !dashboardState.isLoading,
                            colors = if (isCheckedInToSelectedSession) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (dashboardState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = if (isCheckedInToSelectedSession) Icons.Default.ExitToApp else Icons.Default.Wifi,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isCheckedInToSelectedSession) "Check Out via WiFi" else "Check In via WiFi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Information Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "WiFi Check-In Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "• Connect to an authorized WiFi network\n" +
                              "• Ensure you're in the same network as the organization\n" +
                              "• Tap 'Check In via WiFi' when connected\n" +
                              "• Your attendance will be recorded automatically",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Remove success/error messages since we navigate to result screen
        }
    }

    // Session Selection Dialog
    if (showSessionSelector) {
        SessionSelectionDialog(
            sessions = dashboardState.activeSessions,
            onSessionSelected = { session ->
                selectedSession = session
                showSessionSelector = false
            },
            onDismiss = { showSessionSelector = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSelectionDialog(
    sessions: List<ActiveSession>,
    onSessionSelected: (ActiveSession) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Session",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (sessions.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No active sessions available",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Please contact your administrator to start a session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(sessions) { session ->
                        SessionSelectionCard(
                            session = session,
                            onSelected = { onSessionSelected(session) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SessionSelectionCard(
    session: ActiveSession,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            session.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = "Started: ${session.startTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Allowed methods chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(session.allowedMethods) { method ->
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = getMethodIcon(method),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun getEnhancedWifiNetworkInfo(context: Context): Triple<String?, Boolean, Int?> {
    try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo

            if (wifiInfo != null && wifiInfo.networkId != -1) {
                val ssid = wifiInfo.ssid?.replace("\"", "") // Remove quotes
                val signalStrength = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5) // 0-4 scale
                android.util.Log.d("WifiCheckIn", "Connected to: $ssid, Signal: $signalStrength, RSSI: ${wifiInfo.rssi}")
                return Triple(ssid, true, signalStrength)
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("WifiCheckIn", "Error getting WiFi info: ${e.message}")
    }

    return Triple(null, false, null)
}

private fun getWifiNetworkInfo(context: Context): Pair<String?, Boolean> {
    val enhanced = getEnhancedWifiNetworkInfo(context)
    return Pair(enhanced.first, enhanced.second)
}

private fun getSignalIcon(signalLevel: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when (signalLevel) {
        0 -> Icons.Default.SignalWifiOff
        1 -> Icons.Default.Wifi
        2 -> Icons.Default.Wifi
        3 -> Icons.Default.Wifi
        4 -> Icons.Default.Wifi
        else -> Icons.Default.SignalWifiOff
    }
}

private fun getSignalStrengthText(signalLevel: Int): String {
    return when (signalLevel) {
        0 -> "Very Weak"
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        4 -> "Excellent"
        else -> "No Signal"
    }
}

fun getMethodIcon(method: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (method.uppercase()) {
        "QR", "QR_CODE" -> Icons.Default.QrCode
        "WIFI", "WI-FI" -> Icons.Default.Wifi
        "NFC" -> Icons.Default.Nfc
        "BLUETOOTH" -> Icons.Default.Bluetooth
        "MOBILE_NFC" -> Icons.Default.PhoneAndroid
        else -> Icons.Default.CheckCircle
    }
}

fun getMethodDisplayName(method: String): String {
    return when (method.uppercase()) {
        "QR", "QR_CODE" -> "QR Code"
        "WIFI", "WI-FI" -> "Wi-Fi"
        "NFC" -> "NFC"
        "BLUETOOTH" -> "Bluetooth"
        "MOBILE_NFC" -> "Mobile NFC"
        else -> method
    }
}
