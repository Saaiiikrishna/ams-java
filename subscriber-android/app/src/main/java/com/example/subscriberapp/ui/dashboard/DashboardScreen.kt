package com.example.subscriberapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("qr_scanner") {
            QrScannerScreen(
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

            // Quick Actions
            item {
                QuickActionsCard(
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToSessions = onNavigateToSessions,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToQrScanner = onNavigateToQrScanner
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Welcome back, $userName!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = organizationName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun QuickActionsCard(
    onNavigateToProfile: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToQrScanner: () -> Unit
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
                    icon = Icons.Default.EventAvailable,
                    label = "Sessions",
                    onClick = onNavigateToSessions
                )
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
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
                    text = attendance.method,
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
