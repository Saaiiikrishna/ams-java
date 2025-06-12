package com.example.subscriberapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.subscriberapp.ui.auth.AuthViewModel
import com.example.subscriberapp.ui.auth.LoginScreen
import com.example.subscriberapp.ui.dashboard.DashboardScreen
import com.example.subscriberapp.ui.theme.SubscriberAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle deep link for QR code check-in
        handleDeepLink(intent)
        
        setContent {
            SubscriberAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                    
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "dashboard" else "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable("dashboard") {
                            DashboardScreen(
                                onLogout = {
                                    authViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "ams" && uri.host == "checkin") {
                val qrCode = uri.getQueryParameter("qr")
                if (qrCode != null) {
                    // Handle QR code check-in
                    // This would trigger the check-in process
                    // For now, we'll just log it
                    android.util.Log.d("MainActivity", "QR Code received: $qrCode")
                }
            }
        }
    }
}
