package com.example.subscriberapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToQrScanner: (Long) -> Unit = {},
    onNavigateToWifiCheckIn: (Long) -> Unit = {},
    currentUser: com.example.subscriberapp.data.model.Subscriber? = null,
    currentOrganization: com.example.subscriberapp.data.model.Organization? = null
) {
    val availableSessions by dashboardViewModel.availableSessions.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()

    // Use active sessions from dashboard state instead of separate available sessions
    val activeSessions = dashboardState.activeSessions

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Sessions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeSessions.isEmpty() && !dashboardState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Active Sessions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "There are currently no active sessions available for check-in.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(activeSessions) { activeSession ->
                    EnhancedActiveSessionCard(
                        activeSession = activeSession,
                        currentCheckInStatus = dashboardState.currentCheckInStatus,
                        currentUser = currentUser,
                        currentOrganization = currentOrganization,
                        dashboardViewModel = dashboardViewModel,
                        onNavigateToQrScanner = onNavigateToQrScanner,
                        onNavigateToWifiCheckIn = onNavigateToWifiCheckIn
                    )
                }
            }

            if (dashboardState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            dashboardState.errorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSessionCard(
    activeSession: ActiveSession,
    currentCheckInStatus: CheckInStatus?,
    onCheckIn: () -> Unit
) {
    // Check if user is already checked in to this session
    val isCheckedInToThisSession = currentCheckInStatus?.sessionId == activeSession.id
    val isCheckedInToAnySession = currentCheckInStatus?.isCheckedIn == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Session name with status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeSession.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    activeSession.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Session details
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Started: ${activeSession.startTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Show check-in status for this session
                    if (isCheckedInToThisSession) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "You are checked in",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Check-in/Check-out button
                Button(
                    onClick = onCheckIn,
                    modifier = Modifier.padding(start = 12.dp),
                    enabled = !isCheckedInToAnySession || isCheckedInToThisSession,
                    colors = if (isCheckedInToThisSession) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(
                        if (isCheckedInToThisSession) "Check Out"
                        else if (isCheckedInToAnySession) "Already Checked In"
                        else "Check In"
                    )
                }
            }

            // Show allowed check-in methods
            if (activeSession.allowedMethods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Available Check-in Methods:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeSession.allowedMethods.forEach { method ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = getMethodDisplayName(method),
                                    style = MaterialTheme.typography.bodySmall
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

            // Show QR code availability
            activeSession.qrCode?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "QR Code available for scanning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: com.example.subscriberapp.data.model.Session,
    currentCheckInStatus: CheckInStatus?,
    onCheckIn: () -> Unit
) {
    // Check if user is already checked in to this session
    val isCheckedInToThisSession = currentCheckInStatus?.sessionId == session.id
    val isCheckedInToAnySession = currentCheckInStatus?.isCheckedIn == true

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Text(
                        text = "Started: ${session.startTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Show check-in status for this session
                    if (isCheckedInToThisSession) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Currently checked in",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = onCheckIn,
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !isCheckedInToAnySession || isCheckedInToThisSession,
                    colors = if (isCheckedInToThisSession) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(
                        if (isCheckedInToThisSession) "Check Out"
                        else if (isCheckedInToAnySession) "Already Checked In"
                        else "Check In"
                    )
                }
            }

            // Show allowed check-in methods
            if (session.allowedMethods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Available Methods:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    session.allowedMethods.forEach { method ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = getMethodDisplayName(method),
                                    style = MaterialTheme.typography.bodySmall
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
}

@Composable
fun EnhancedActiveSessionCard(
    activeSession: ActiveSession,
    currentCheckInStatus: CheckInStatus?,
    currentUser: com.example.subscriberapp.data.model.Subscriber?,
    currentOrganization: com.example.subscriberapp.data.model.Organization?,
    dashboardViewModel: DashboardViewModel,
    onNavigateToQrScanner: (Long) -> Unit,
    onNavigateToWifiCheckIn: (Long) -> Unit
) {
    // Check if user is already checked in to this session
    val isCheckedInToThisSession = currentCheckInStatus?.sessionId == activeSession.id
    val isCheckedInToAnySession = currentCheckInStatus?.isCheckedIn == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with session name and check-in button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Session name with status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeSession.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCheckedInToThisSession)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = if (isCheckedInToThisSession) "CHECKED IN" else "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCheckedInToThisSession)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    activeSession.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Session details
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Started: ${activeSession.startTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Main Check-in/Check-out button (top right)
                Button(
                    onClick = {
                        if (isCheckedInToThisSession) {
                            // Handle check-out - use WiFi method for now
                            currentUser?.let { user ->
                                currentOrganization?.let { org ->
                                    android.util.Log.d("SessionCard", "Attempting check-out for session: ${activeSession.id}")
                                    dashboardViewModel.performWifiCheckInForSession(
                                        user.mobileNumber,
                                        org.entityId,
                                        "AndroidWifi", // Use a default network name for check-out
                                        activeSession.id
                                    )
                                }
                            }
                        } else if (isCheckedInToAnySession) {
                            // User is checked in to another session - show error or navigate to check-out
                            android.util.Log.d("SessionCard", "User is checked in to another session")
                            // Could show a dialog or navigate to current session for check-out
                        } else {
                            // Navigate to first available method or show options
                            val firstMethod = activeSession.allowedMethods.firstOrNull()
                            when (firstMethod?.uppercase()) {
                                "QR", "QR_CODE" -> onNavigateToQrScanner(activeSession.id)
                                "WIFI", "WI-FI" -> onNavigateToWifiCheckIn(activeSession.id)
                                else -> {
                                    // Default to QR if available, otherwise WiFi
                                    if (activeSession.allowedMethods.any { it.contains("QR", ignoreCase = true) }) {
                                        onNavigateToQrScanner(activeSession.id)
                                    } else if (activeSession.allowedMethods.any { it.contains("WIFI", ignoreCase = true) }) {
                                        onNavigateToWifiCheckIn(activeSession.id)
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 12.dp),
                    enabled = !isCheckedInToAnySession || isCheckedInToThisSession,
                    colors = if (isCheckedInToThisSession) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isCheckedInToThisSession) Icons.Default.ExitToApp else Icons.Default.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCheckedInToThisSession) "Check Out"
                        else if (isCheckedInToAnySession) "Checked In"
                        else "Check In",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Check-in methods section
            if (activeSession.allowedMethods.isNotEmpty() && !isCheckedInToAnySession) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Check-in Methods:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeSession.allowedMethods) { method ->
                        CheckInMethodButton(
                            method = method,
                            session = activeSession,
                            currentUser = currentUser,
                            currentOrganization = currentOrganization,
                            dashboardViewModel = dashboardViewModel,
                            onNavigateToQrScanner = onNavigateToQrScanner,
                            onNavigateToWifiCheckIn = onNavigateToWifiCheckIn
                        )
                    }
                }
            }

            // Show QR code availability
            if (activeSession.qrCode != null && !isCheckedInToAnySession) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "QR Code ready for scanning",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckInMethodButton(
    method: String,
    session: ActiveSession,
    currentUser: com.example.subscriberapp.data.model.Subscriber?,
    currentOrganization: com.example.subscriberapp.data.model.Organization?,
    dashboardViewModel: DashboardViewModel,
    onNavigateToQrScanner: (Long) -> Unit,
    onNavigateToWifiCheckIn: (Long) -> Unit
) {
    val methodIcon = getMethodIcon(method)
    val methodName = getMethodDisplayName(method)

    Button(
        onClick = {
            when (method.uppercase()) {
                "QR", "QR_CODE" -> onNavigateToQrScanner(session.id)
                "WIFI", "WI-FI" -> onNavigateToWifiCheckIn(session.id)
                "NFC" -> {
                    // TODO: Implement NFC check-in
                }
                "BLUETOOTH" -> {
                    // TODO: Implement Bluetooth check-in
                }
                "MOBILE_NFC" -> {
                    // TODO: Implement Mobile NFC check-in
                }
                else -> {
                    // Default action or show unsupported message
                }
            }
        },
        modifier = Modifier.width(120.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = methodIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = methodName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
