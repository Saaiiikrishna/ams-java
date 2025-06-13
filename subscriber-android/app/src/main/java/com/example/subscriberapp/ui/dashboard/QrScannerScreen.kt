package com.example.subscriberapp.ui.dashboard

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.subscriberapp.ui.auth.AuthViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentOrganization by authViewModel.currentOrganization.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Code Scanner") },
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
        ) {
            when {
                cameraPermissionState.status.isGranted -> {
                    // Camera permission granted, show camera
                    QrCodeCameraPreview(
                        onQrCodeScanned = { qrCode ->
                            currentUser?.let { user ->
                                currentOrganization?.let { org ->
                                    dashboardViewModel.performQrCheckIn(
                                        user.mobileNumber,
                                        org.entityId,
                                        qrCode
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                cameraPermissionState.status.shouldShowRationale -> {
                    // Show rationale and request permission
                    CameraPermissionRationale(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }
                else -> {
                    // Request permission
                    CameraPermissionRequest(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }
            }

            // Status and instructions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (dashboardState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Processing check-in...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Point your camera at a QR code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Make sure the QR code is clearly visible and well-lit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Show success/error messages
                    dashboardState.successMessage?.let { message ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    dashboardState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Clear messages after some time
    LaunchedEffect(dashboardState.successMessage, dashboardState.errorMessage) {
        if (dashboardState.successMessage != null || dashboardState.errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            dashboardViewModel.clearMessages()
        }
    }
}

@Composable
fun QrCodeCameraPreview(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Debug logging
    android.util.Log.d("QrScanner", "QrCodeCameraPreview initialized")
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScanTime by remember { mutableStateOf(0L) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                android.util.Log.d("QrScanner", "Camera provider listener triggered")
                val cameraProvider = cameraProviderFuture.get()
                android.util.Log.d("QrScanner", "Camera provider obtained")

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                android.util.Log.d("QrScanner", "Preview configured")

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val barcodeScanner = BarcodeScanning.getClient()
                android.util.Log.d("QrScanner", "Barcode scanner initialized")

                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    android.util.Log.d("QrScanner", "Image analysis frame received")
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                android.util.Log.d("QrScanner", "Barcodes detected: ${barcodes.size}")
                                for (barcode in barcodes) {
                                    android.util.Log.d("QrScanner", "Barcode format: ${barcode.format}")
                                    android.util.Log.d("QrScanner", "Barcode raw value: ${barcode.rawValue}")
                                    barcode.rawValue?.let { rawQrCode ->
                                        val currentTime = System.currentTimeMillis()
                                        android.util.Log.d("QrScanner", "QR Code detected: $rawQrCode")

                                        // Extract QR parameter from URL if it's in the expected format
                                        val qrCode = extractQrParameter(rawQrCode)
                                        android.util.Log.d("QrScanner", "Extracted QR parameter: $qrCode")

                                        // Prevent duplicate scans within 2 seconds
                                        if (qrCode != lastScannedCode || currentTime - lastScanTime > 2000) {
                                            lastScannedCode = qrCode
                                            lastScanTime = currentTime
                                            android.util.Log.d("QrScanner", "Processing QR Code: $qrCode")
                                            onQrCodeScanned(qrCode)
                                        } else {
                                            android.util.Log.d("QrScanner", "Duplicate QR Code ignored")
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                android.util.Log.e("QrScanner", "Barcode scanning failed", exception)
                                imageProxy.close()
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    android.util.Log.d("QrScanner", "Binding camera to lifecycle")
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    android.util.Log.d("QrScanner", "Camera bound successfully")
                } catch (exc: Exception) {
                    android.util.Log.e("QrScanner", "Camera binding failed", exc)
                }
            }, executor)
            
            previewView
        },
        modifier = modifier
    )
}

/**
 * Extract QR parameter from URL format: ams://checkin?qr=<encoded_data>
 * Returns the encoded_data part, or the original string if not in expected format
 */
private fun extractQrParameter(rawQrCode: String): String {
    return try {
        if (rawQrCode.startsWith("ams://checkin?qr=")) {
            // Extract the parameter after "qr="
            val qrParam = rawQrCode.substringAfter("qr=")
            android.util.Log.d("QrScanner", "Extracted QR parameter from URL: $qrParam")
            qrParam
        } else {
            // If not in expected URL format, return as-is (might be direct encoded data)
            android.util.Log.d("QrScanner", "QR code not in URL format, using as-is: $rawQrCode")
            rawQrCode
        }
    } catch (e: Exception) {
        android.util.Log.e("QrScanner", "Error extracting QR parameter: ${e.message}")
        rawQrCode // Return original on error
    }
}

@Composable
fun CameraPermissionRequest(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "To scan QR codes for attendance, we need access to your camera.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Camera Permission")
        }
    }
}

@Composable
fun CameraPermissionRationale(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera Access Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "The camera is essential for scanning QR codes to mark your attendance. Please grant permission to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow Camera Access")
        }
    }
}
