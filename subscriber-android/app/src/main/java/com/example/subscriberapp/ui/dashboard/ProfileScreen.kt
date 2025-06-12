package com.example.subscriberapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.subscriberapp.data.model.Subscriber
import com.example.subscriberapp.data.model.Organization
import com.example.subscriberapp.ui.auth.PinChangeDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: Subscriber?,
    currentOrganization: Organization?,
    onNavigateBack: () -> Unit
) {
    var showPinChangeDialog by remember { mutableStateOf(false) }

    // Debug logging
    android.util.Log.d("ProfileScreen", "currentUser: $currentUser")
    android.util.Log.d("ProfileScreen", "currentOrganization: $currentOrganization")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            currentUser?.let { user ->
                // Profile Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${user.firstName} ${user.lastName}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Profile Details
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        ProfileDetailRow(
                            icon = Icons.Default.Person,
                            label = "First Name",
                            value = user.firstName
                        )

                        ProfileDetailRow(
                            icon = Icons.Default.Person,
                            label = "Last Name",
                            value = user.lastName
                        )

                        ProfileDetailRow(
                            icon = Icons.Default.Phone,
                            label = "Mobile Number",
                            value = user.mobileNumber
                        )

                        user.email?.let { email ->
                            ProfileDetailRow(
                                icon = Icons.Default.Email,
                                label = "Email",
                                value = email
                            )
                        }

                        ProfileDetailRow(
                            icon = if (user.hasNfcCard) Icons.Default.CreditCard else Icons.Default.CreditCardOff,
                            label = "NFC Card",
                            value = if (user.hasNfcCard) "Assigned" else "Not Assigned"
                        )
                    }
                }

                // Organization Information
                currentOrganization?.let { organization ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Organization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            ProfileDetailRow(
                                icon = Icons.Default.Business,
                                label = "Organization Name",
                                value = organization.name
                            )

                            ProfileDetailRow(
                                icon = Icons.Default.Badge,
                                label = "Entity ID",
                                value = organization.entityId
                            )

                            organization.address?.let { address ->
                                ProfileDetailRow(
                                    icon = Icons.Default.LocationOn,
                                    label = "Address",
                                    value = address
                                )
                            }
                        }
                    }
                }

                // Account Actions
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Account Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedButton(
                            onClick = { showPinChangeDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change PIN")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { /* TODO: Implement profile update */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update Profile")
                        }
                    }
                }
            } ?: run {
                // Loading or error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No user data available")
                }
            }
        }
    }

    // PIN Change Dialog
    if (showPinChangeDialog) {
        PinChangeDialog(
            onDismiss = { showPinChangeDialog = false },
            onPinChanged = {
                showPinChangeDialog = false
                // You could show a success message here if needed
            }
        )
    }
}

@Composable
fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
