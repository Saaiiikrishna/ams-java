package com.example.subscriberapp.ui.dashboard

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import com.example.subscriberapp.data.model.Subscriber
import com.example.subscriberapp.data.model.Organization

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EnhancedQrScannerScreen(
    currentUser: com.example.subscriberapp.data.model.Subscriber?,
    currentOrganization: com.example.subscriberapp.data.model.Organization?,
    onNavigateBack: () -> Unit,
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var isScanning by remember { mutableStateOf(true) }
    var detectedQrCode by remember { mutableStateOf<String?>(null) }
    var scanningState by remember { mutableStateOf(ScanningState.SCANNING) }
    var zoomLevel by remember { mutableStateOf(0f) }
    var isFlashOn by remember { mutableStateOf(false) }

    val dashboardState by dashboardViewModel.dashboardState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Code Scanner") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Flash toggle
                    IconButton(onClick = { isFlashOn = !isFlashOn }) {
                        Icon(
                            if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (isFlashOn) "Turn off flash" else "Turn on flash",
                            tint = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                cameraPermissionState.status.isGranted -> {
                    // Enhanced Camera with QR Detection
                    EnhancedQrCameraPreview(
                        onQrCodeDetected = { qrCode ->
                            if (isScanning && detectedQrCode != qrCode) {
                                detectedQrCode = qrCode
                                scanningState = ScanningState.DETECTED
                                isScanning = false
                                
                                // Process QR code
                                currentUser?.let { user ->
                                    currentOrganization?.let { org ->
                                        dashboardViewModel.performQrCheckIn(
                                            user.mobileNumber,
                                            org.entityId,
                                            qrCode
                                        )
                                        scanningState = ScanningState.PROCESSING
                                    }
                                }
                            }
                        },
                        isFlashOn = isFlashOn,
                        zoomLevel = zoomLevel,
                        onZoomChange = { zoomLevel = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    // QR Detection Overlay
                    QrDetectionOverlay(
                        scanningState = scanningState,
                        detectedQrCode = detectedQrCode,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Camera Controls
                    CameraControlsOverlay(
                        zoomLevel = zoomLevel,
                        onZoomChange = { zoomLevel = it },
                        onResetScan = {
                            isScanning = true
                            detectedQrCode = null
                            scanningState = ScanningState.SCANNING
                            dashboardViewModel.clearMessages()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Status Messages
                    StatusMessageOverlay(
                        dashboardState = dashboardState,
                        scanningState = scanningState,
                        onDismiss = {
                            dashboardViewModel.clearMessages()
                            isScanning = true
                            detectedQrCode = null
                            scanningState = ScanningState.SCANNING
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    // Debug Info Overlay
                    DebugInfoOverlay(
                        currentUser = currentUser,
                        currentOrganization = currentOrganization,
                        dashboardState = dashboardState,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

                    // Manual QR Input Overlay
                    ManualQrInputOverlay(
                        currentUser = currentUser,
                        currentOrganization = currentOrganization,
                        dashboardViewModel = dashboardViewModel,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }

                !cameraPermissionState.status.isGranted -> {
                    // Permission handling
                    when {
                        cameraPermissionState.status.shouldShowRationale -> {
                            CameraPermissionRationale(
                                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                            )
                        }
                        else -> {
                            CameraPermissionRequest(
                                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class ScanningState {
    SCANNING,
    DETECTED,
    PROCESSING,
    SUCCESS,
    ERROR
}

@Composable
fun EnhancedQrCameraPreview(
    onQrCodeDetected: (String) -> Unit,
    isFlashOn: Boolean,
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScanTime by remember { mutableStateOf(0L) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            android.util.Log.d("EnhancedQrScanner", "Initializing enhanced camera preview")
            
            cameraProviderFuture.addListener({
                android.util.Log.d("EnhancedQrScanner", "Camera provider ready")
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val barcodeScanner = BarcodeScanning.getClient()
                android.util.Log.d("EnhancedQrScanner", "Barcode scanner initialized")

                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { qrCode ->
                                        val currentTime = System.currentTimeMillis()
                                        android.util.Log.d("EnhancedQrScanner", "QR Code detected: $qrCode")
                                        
                                        // Prevent duplicate scans within 3 seconds
                                        if (qrCode != lastScannedCode || currentTime - lastScanTime > 3000) {
                                            lastScannedCode = qrCode
                                            lastScanTime = currentTime
                                            android.util.Log.d("EnhancedQrScanner", "Processing QR Code: $qrCode")
                                            onQrCodeDetected(qrCode)
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                android.util.Log.e("EnhancedQrScanner", "Barcode scanning failed", exception)
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
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    android.util.Log.d("EnhancedQrScanner", "Camera bound successfully")
                } catch (exc: Exception) {
                    android.util.Log.e("EnhancedQrScanner", "Camera binding failed", exc)
                }
            }, executor)
            
            previewView
        },
        modifier = modifier,
        update = { previewView ->
            // Update camera settings
            camera?.let { cam ->
                // Set zoom level
                cam.cameraControl.setZoomRatio(1f + zoomLevel * 3f) // 1x to 4x zoom

                // Set flash
                cam.cameraControl.enableTorch(isFlashOn)

                android.util.Log.d("EnhancedQrScanner", "Camera settings updated - Zoom: ${1f + zoomLevel * 3f}, Flash: $isFlashOn")
            }
        }
    )
}

@Composable
fun QrDetectionOverlay(
    scanningState: ScanningState,
    detectedQrCode: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Semi-transparent overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate scanning frame
            val frameSize = minOf(canvasWidth, canvasHeight) * 0.7f
            val frameLeft = (canvasWidth - frameSize) / 2
            val frameTop = (canvasHeight - frameSize) / 2

            // Draw semi-transparent overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(canvasWidth, canvasHeight)
            )

            // Cut out the scanning area
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Draw scanning frame
            val frameColor = when (scanningState) {
                ScanningState.SCANNING -> Color.White
                ScanningState.DETECTED -> Color.Green
                ScanningState.PROCESSING -> Color.Yellow
                ScanningState.SUCCESS -> Color.Green
                ScanningState.ERROR -> Color.Red
            }

            drawRoundRect(
                color = frameColor,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw corner indicators
            val cornerLength = 30.dp.toPx()
            val cornerWidth = 6.dp.toPx()

            // Top-left corner
            drawLine(
                color = frameColor,
                start = Offset(frameLeft, frameTop + cornerLength),
                end = Offset(frameLeft, frameTop),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft, frameTop),
                end = Offset(frameLeft + cornerLength, frameTop),
                strokeWidth = cornerWidth
            )

            // Top-right corner
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameSize - cornerLength, frameTop),
                end = Offset(frameLeft + frameSize, frameTop),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameSize, frameTop),
                end = Offset(frameLeft + frameSize, frameTop + cornerLength),
                strokeWidth = cornerWidth
            )

            // Bottom-left corner
            drawLine(
                color = frameColor,
                start = Offset(frameLeft, frameTop + frameSize - cornerLength),
                end = Offset(frameLeft, frameTop + frameSize),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft, frameTop + frameSize),
                end = Offset(frameLeft + cornerLength, frameTop + frameSize),
                strokeWidth = cornerWidth
            )

            // Bottom-right corner
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameSize - cornerLength, frameTop + frameSize),
                end = Offset(frameLeft + frameSize, frameTop + frameSize),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameSize, frameTop + frameSize - cornerLength),
                end = Offset(frameLeft + frameSize, frameTop + frameSize),
                strokeWidth = cornerWidth
            )
        }

        // Instruction text
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val instructionText = when (scanningState) {
                ScanningState.SCANNING -> "Position QR code within the frame"
                ScanningState.DETECTED -> "QR Code detected!"
                ScanningState.PROCESSING -> "Processing attendance..."
                ScanningState.SUCCESS -> "Attendance recorded successfully!"
                ScanningState.ERROR -> "Failed to record attendance"
            }

            Text(
                text = instructionText,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (scanningState == ScanningState.DETECTED && detectedQrCode != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Code: ${detectedQrCode.take(20)}...",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CameraControlsOverlay(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    onResetScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ZoomOut,
                    contentDescription = "Zoom out",
                    tint = Color.White
                )
                Slider(
                    value = zoomLevel,
                    onValueChange = onZoomChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Icon(
                    Icons.Default.ZoomIn,
                    contentDescription = "Zoom in",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Zoom: ${String.format("%.1f", 1f + zoomLevel * 3f)}x",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reset scan button
            OutlinedButton(
                onClick = onResetScan,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Again")
            }
        }
    }
}

@Composable
fun StatusMessageOverlay(
    dashboardState: DashboardState,
    scanningState: ScanningState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (dashboardState.successMessage != null || dashboardState.errorMessage != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (dashboardState.successMessage != null) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (dashboardState.successMessage != null) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (dashboardState.successMessage != null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dashboardState.successMessage ?: dashboardState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (dashboardState.successMessage != null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dashboardState.successMessage != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                ) {
                    Text("Continue Scanning")
                }
            }
        }
    }
}

@Composable
fun DebugInfoOverlay(
    currentUser: Subscriber?,
    currentOrganization: Organization?,
    dashboardState: DashboardState,
    modifier: Modifier = Modifier
) {
    var showDebugInfo by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Toggle button
        OutlinedButton(
            onClick = { showDebugInfo = !showDebugInfo },
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            Text(
                text = "?",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (showDebugInfo) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Debug Info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "User: ${currentUser?.mobileNumber ?: "null"}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "Entity: ${currentOrganization?.entityId ?: "null"}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "Active Sessions: ${dashboardState.activeSessions.size}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (dashboardState.activeSessions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sessions:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        dashboardState.activeSessions.forEach { session: ActiveSession ->
                            Text(
                                text = "• ${session.name} (ID: ${session.id})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            session.qrCode?.let { qr: String ->
                                Text(
                                    text = "  QR: ${qr.take(20)}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            } ?: Text(
                                text = "  QR: null",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManualQrInputOverlay(
    currentUser: Subscriber?,
    currentOrganization: Organization?,
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    var showManualInput by remember { mutableStateOf(false) }
    var qrCodeInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Toggle button
        FloatingActionButton(
            onClick = { showManualInput = !showManualInput },
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Manual QR Input"
            )
        }

        if (showManualInput) {
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Manual QR Input",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = qrCodeInput,
                        onValueChange = { qrCodeInput = it },
                        label = { Text("QR Code") },
                        placeholder = { Text("Enter QR code...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showManualInput = false
                                qrCodeInput = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (qrCodeInput.isNotBlank()) {
                                    currentUser?.let { user ->
                                        currentOrganization?.let { org ->
                                            android.util.Log.d("ManualQrInput", "Testing QR code: $qrCodeInput")
                                            dashboardViewModel.performQrCheckIn(
                                                user.mobileNumber,
                                                org.entityId,
                                                qrCodeInput.trim()
                                            )
                                        }
                                    }
                                    showManualInput = false
                                    qrCodeInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = qrCodeInput.isNotBlank()
                        ) {
                            Text("Test")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick test buttons for common QR codes
                    Text(
                        text = "Quick Tests:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { qrCodeInput = "24" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Session ID: 24", style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = { qrCodeInput = "Morning meet" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Session Name", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
