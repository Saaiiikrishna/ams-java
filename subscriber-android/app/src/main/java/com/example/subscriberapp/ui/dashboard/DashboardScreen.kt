package com.example.subscriberapp.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.subscriberapp.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentOrganization by authViewModel.currentOrganization.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()

    // Debug logging
    android.util.Log.d("DashboardScreen", "currentUser: $currentUser")
    android.util.Log.d("DashboardScreen", "currentOrganization: $currentOrganization")

    LaunchedEffect(currentUser, currentOrganization) {
        currentUser?.let { user ->
            currentOrganization?.let { org ->
                dashboardViewModel.loadDashboard(user.mobileNumber, org.entityId)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "main_dashboard"
    ) {
        composable("main_dashboard") {
            MainDashboardContent(
                currentUser = currentUser,
                currentOrganization = currentOrganization,
                dashboardState = dashboardState,
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToSessions = { navController.navigate("sessions") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToQrScanner = { navController.navigate("qr_scanner") },
                onNavigateToWifiCheckIn = { navController.navigate("wifi_checkin") },
                onLogout = onLogout,
                onRefresh = {
                    currentUser?.let { user ->
                        currentOrganization?.let { org ->
                            dashboardViewModel.loadDashboard(user.mobileNumber, org.entityId)
                        }
                    }
                }
            )
        }
        
        composable("profile") {
            ProfileScreen(
                currentUser = currentUser,
                currentOrganization = currentOrganization,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("sessions") {
            SessionsScreen(
                dashboardViewModel = dashboardViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("history") {
            AttendanceHistoryScreen(
                dashboardViewModel = dashboardViewModel,
                currentUser = currentUser,
                currentOrganization = currentOrganization,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("qr_scanner") {
            EnhancedQrScannerScreen(
                currentUser = currentUser,
                currentOrganization = currentOrganization,
                dashboardViewModel = dashboardViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("wifi_checkin") {
            WifiCheckInScreen(
                currentUser = currentUser,
                currentOrganization = currentOrganization,
                dashboardViewModel = dashboardViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardContent(
    currentUser: com.example.subscriberapp.data.model.Subscriber?,
    currentOrganization: com.example.subscriberapp.data.model.Organization?,
    dashboardState: DashboardState,
    onNavigateToProfile: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    onNavigateToWifiCheckIn: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = currentOrganization?.name ?: "Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card
            item {
                WelcomeCard(
                    userName = currentUser?.let { "${it.firstName} ${it.lastName}" } ?: "User",
                    organizationName = currentOrganization?.name ?: "Organization"
                )
            }

            // Current Check-In Status Card
            dashboardState.currentCheckInStatus?.let { checkInStatus ->
                item {
                    CurrentCheckInStatusCard(
                        checkInStatus = checkInStatus,
                        onCheckOut = { method ->
                            currentUser?.let { user ->
                                currentOrganization?.let { org ->
                                    when (method) {
                                        "QR" -> onNavigateToQrScanner()
                                        "WiFi" -> onNavigateToWifiCheckIn()
                                        else -> {
                                            // For NFC and other methods, show info
                                            // Could implement specific check-out flows
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Quick Actions
            item {
                QuickActionsCard(
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToSessions = onNavigateToSessions,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToQrScanner = onNavigateToQrScanner,
                    onNavigateToWifiCheckIn = onNavigateToWifiCheckIn
                )
            }

            // Active Sessions
            if (dashboardState.activeSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Sessions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(dashboardState.activeSessions) { session ->
                    ActiveSessionCard(session = session)
                }
            }

            // Recent Attendance
            if (dashboardState.recentAttendance.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Attendance",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(dashboardState.recentAttendance.take(5)) { attendance ->
                    AttendanceCard(attendance = attendance)
                }
            }

            // Upcoming Sessions
            if (dashboardState.upcomingSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Upcoming Sessions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(dashboardState.upcomingSessions.take(3)) { session ->
                    UpcomingSessionCard(session = session)
                }
            }

            // Loading State
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

            // Error State
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
fun WelcomeCard(
    userName: String,
    organizationName: String
) {
    // Animation for welcome card
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
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
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = organizationName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onNavigateToProfile: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    onNavigateToWifiCheckIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // First row of actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan QR",
                    onClick = onNavigateToQrScanner
                )
                QuickActionButton(
                    icon = Icons.Default.Wifi,
                    label = "WiFi Check-In",
                    onClick = onNavigateToWifiCheckIn
                )
                QuickActionButton(
                    icon = Icons.Default.EventAvailable,
                    label = "Sessions",
                    onClick = onNavigateToSessions
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Second row of actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Default.History,
                    label = "History",
                    onClick = onNavigateToHistory
                )
                QuickActionButton(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    onClick = onNavigateToProfile
                )
                // Empty space for symmetry
                Spacer(modifier = Modifier.width(56.dp))
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
fun CurrentCheckInStatusCard(
    checkInStatus: CheckInStatus,
    onCheckOut: (String) -> Unit
) {
    val timeAgo = remember(checkInStatus.checkInTime) {
        // Simple time ago calculation - you can enhance this
        "Active now"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.primary
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Currently Checked In",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = checkInStatus.sessionName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (checkInStatus.checkInMethod) {
                                    "QR" -> Icons.Default.QrCodeScanner
                                    "WiFi" -> Icons.Default.Wifi
                                    "NFC" -> Icons.Default.Nfc
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${checkInStatus.checkInMethod} • $timeAgo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // Check-out button
                    Button(
                        onClick = { onCheckOut(checkInStatus.checkInMethod) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Check Out",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSessionCard(session: ActiveSession) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
            Text(
                text = "Methods: ${session.allowedMethods.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AttendanceCard(attendance: AttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = attendance.sessionName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Check-in: ${attendance.checkInTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                attendance.checkOutTime?.let { checkOut ->
                    Text(
                        text = "Check-out: $checkOut",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = attendance.checkInMethod,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = attendance.status.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (attendance.status == "completed")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun UpcomingSessionCard(session: UpcomingSession) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                text = "Time: ${session.startTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Duration: ${session.durationMinutes} minutes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Days: ${session.daysOfWeek.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
