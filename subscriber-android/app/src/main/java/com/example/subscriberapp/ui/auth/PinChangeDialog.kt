package com.example.subscriberapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinChangeDialog(
    onDismiss: () -> Unit,
    onPinChanged: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showCurrentPin by remember { mutableStateOf(false) }
    var showNewPin by remember { mutableStateOf(false) }
    var showConfirmPin by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle success
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage?.contains("PIN updated successfully") == true) {
            onPinChanged()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change PIN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter your current PIN and choose a new 4-digit PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Current PIN Field
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            currentPin = newValue
                        }
                    },
                    label = { Text("Current PIN") },
                    placeholder = { Text("0000") },
                    visualTransformation = if (showCurrentPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPin = !showCurrentPin }) {
                            Icon(
                                imageVector = if (showCurrentPin) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (showCurrentPin) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // New PIN Field
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            newPin = newValue
                        }
                    },
                    label = { Text("New PIN") },
                    placeholder = { Text("0000") },
                    visualTransformation = if (showNewPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { showNewPin = !showNewPin }) {
                            Icon(
                                imageVector = if (showNewPin) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (showNewPin) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Confirm PIN Field
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            confirmPin = newValue
                        }
                    },
                    label = { Text("Confirm New PIN") },
                    placeholder = { Text("0000") },
                    visualTransformation = if (showConfirmPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPin = !showConfirmPin }) {
                            Icon(
                                imageVector = if (showConfirmPin) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (showConfirmPin) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = confirmPin.isNotEmpty() && confirmPin != newPin
                )
                
                // PIN mismatch warning
                if (confirmPin.isNotEmpty() && confirmPin != newPin) {
                    Text(
                        text = "PINs do not match",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Error Message
                uiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin == confirmPin && currentPin.length == 4 && newPin.length == 4) {
                        viewModel.updatePin(currentPin, newPin)
                    }
                },
                enabled = currentPin.length == 4 && 
                         newPin.length == 4 && 
                         confirmPin.length == 4 && 
                         newPin == confirmPin && 
                         !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Update PIN")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
