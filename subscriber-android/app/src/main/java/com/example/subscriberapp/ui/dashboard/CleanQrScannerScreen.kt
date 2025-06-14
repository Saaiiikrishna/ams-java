package com.example.subscriberapp.ui.dashboard

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
fun CleanQrScannerScreen(
    currentUser: Subscriber?,
    currentOrganization: Organization?,
    selectedSessionId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (Boolean, String, String, String) -> Unit,
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var scanningState by remember { mutableStateOf(CleanScanningState.SCANNING) }
    var detectedQrCode by remember { mutableStateOf<String?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    val dashboardState by dashboardViewModel.dashboardState.collectAsState()

    // Handle check-in result
    LaunchedEffect(dashboardState.successMessage, dashboardState.errorMessage) {
        if (dashboardState.successMessage != null) {
            onNavigateToResult(
                true,
                detectedQrCode?.let { qr ->
                    // Try to find session name from active sessions
                    dashboardState.activeSessions.find { it.qrCode == qr }?.name ?: "Session"
                } ?: "Session",
                dashboardState.successMessage!!,
                "QR"
            )
        } else if (dashboardState.errorMessage != null) {
            onNavigateToResult(
                false,
                "Session",
                dashboardState.errorMessage!!,
                "QR"
            )
        }
    }

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
                    // Clean Camera with QR Detection
                    CleanQrCameraPreview(
                        onQrCodeDetected = { qrCode ->
                            if (scanningState == CleanScanningState.SCANNING && detectedQrCode != qrCode) {
                                detectedQrCode = qrCode
                                scanningState = CleanScanningState.DETECTED

                                // Process QR code
                                currentUser?.let { user ->
                                    currentOrganization?.let { org ->
                                        scanningState = CleanScanningState.PROCESSING
                                        if (selectedSessionId != null) {
                                            dashboardViewModel.performQrCheckInForSession(
                                                user.mobileNumber,
                                                org.entityId,
                                                qrCode,
                                                selectedSessionId
                                            )
                                        } else {
                                            dashboardViewModel.performQrCheckIn(
                                                user.mobileNumber,
                                                org.entityId,
                                                qrCode
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        isFlashOn = isFlashOn,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Beautiful QR Detection Overlay
                    BeautifulQrOverlay(
                        scanningState = scanningState,
                        modifier = Modifier.fillMaxSize()
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

enum class CleanScanningState {
    SCANNING,
    DETECTED,
    PROCESSING
}

@Composable
fun CleanQrCameraPreview(
    onQrCodeDetected: (String) -> Unit,
    isFlashOn: Boolean,
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
            
            cameraProviderFuture.addListener({
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
                                    barcode.rawValue?.let { rawQrCode ->
                                        val currentTime = System.currentTimeMillis()
                                        
                                        // Extract QR parameter from URL if needed
                                        val qrCode = extractQrParameter(rawQrCode)
                                        
                                        // Prevent duplicate scans within 3 seconds
                                        if (qrCode != lastScannedCode || currentTime - lastScanTime > 3000) {
                                            lastScannedCode = qrCode
                                            lastScanTime = currentTime
                                            onQrCodeDetected(qrCode)
                                        }
                                    }
                                }
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
                } catch (exc: Exception) {
                    android.util.Log.e("CleanQrScanner", "Camera binding failed", exc)
                }
            }, executor)
            
            previewView
        },
        modifier = modifier,
        update = { previewView ->
            // Update camera settings
            camera?.let { cam ->
                cam.cameraControl.enableTorch(isFlashOn)
            }
        }
    )
}

/**
 * Extract QR parameter from URL format: ams://checkin?qr=<encoded_data>
 * Returns the encoded_data part, or the original string if not in expected format
 */
@Composable
fun BeautifulQrOverlay(
    scanningState: CleanScanningState,
    modifier: Modifier = Modifier
) {
    // Animation for scanning line
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scanLinePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    // Animation for corner indicators
    val cornerAnimation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corners"
    )

    Box(modifier = modifier) {
        // Semi-transparent overlay with scanning frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate scanning frame
            val frameSize = minOf(canvasWidth, canvasHeight) * 0.65f
            val frameLeft = (canvasWidth - frameSize) / 2
            val frameTop = (canvasHeight - frameSize) / 2

            // Draw semi-transparent overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                size = Size(canvasWidth, canvasHeight)
            )

            // Cut out the scanning area
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Frame color based on scanning state
            val frameColor = when (scanningState) {
                CleanScanningState.SCANNING -> Color.White
                CleanScanningState.DETECTED -> Color.Green
                CleanScanningState.PROCESSING -> Color.Yellow
            }

            // Draw scanning frame border
            drawRoundRect(
                color = frameColor,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize),
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw animated corner indicators
            val cornerLength = 40.dp.toPx() * cornerAnimation
            val cornerWidth = 4.dp.toPx()
            val cornerOffset = 10.dp.toPx()

            // Top-left corner
            drawLine(
                color = frameColor,
                start = Offset(frameLeft - cornerOffset, frameTop + cornerLength),
                end = Offset(frameLeft - cornerOffset, frameTop - cornerOffset),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft - cornerOffset, frameTop - cornerOffset),
                end = Offset(frameLeft + cornerLength, frameTop - cornerOffset),
                strokeWidth = cornerWidth
            )

            // Top-right corner
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameSize - cornerLength, frameTop - cornerOffset),
                end = Offset(frameLeft + frameSize + cornerOffset, frameTop - cornerOffset),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameSize + cornerOffset, frameTop - cornerOffset),
                end = Offset(frameLeft + frameSize + cornerOffset, frameTop + cornerLength),
                strokeWidth = cornerWidth
            )

            // Bottom-left corner
            drawLine(
                color = frameColor,
                start = Offset(frameLeft - cornerOffset, frameTop + frameSize - cornerLength),
                end = Offset(frameLeft - cornerOffset, frameTop + frameSize + cornerOffset),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft - cornerOffset, frameTop + frameSize + cornerOffset),
                end = Offset(frameLeft + cornerLength, frameTop + frameSize + cornerOffset),
                strokeWidth = cornerWidth
            )

            // Bottom-right corner
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameSize - cornerLength, frameTop + frameSize + cornerOffset),
                end = Offset(frameLeft + frameSize + cornerOffset, frameTop + frameSize + cornerOffset),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = frameColor,
                start = Offset(frameLeft + frameSize + cornerOffset, frameTop + frameSize - cornerLength),
                end = Offset(frameLeft + frameSize + cornerOffset, frameTop + frameSize + cornerOffset),
                strokeWidth = cornerWidth
            )

            // Draw animated scanning line (only when scanning)
            if (scanningState == CleanScanningState.SCANNING) {
                val scanLineY = frameTop + (frameSize * scanLinePosition)
                drawLine(
                    color = Color.Red.copy(alpha = 0.8f),
                    start = Offset(frameLeft + 20.dp.toPx(), scanLineY),
                    end = Offset(frameLeft + frameSize - 20.dp.toPx(), scanLineY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Instruction text at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val instructionText = when (scanningState) {
                CleanScanningState.SCANNING -> "Position QR code within the frame"
                CleanScanningState.DETECTED -> "QR Code detected!"
                CleanScanningState.PROCESSING -> "Processing attendance..."
            }

            Text(
                text = instructionText,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun extractQrParameter(rawQrCode: String): String {
    return try {
        if (rawQrCode.startsWith("ams://checkin?qr=")) {
            rawQrCode.substringAfter("qr=")
        } else {
            rawQrCode
        }
    } catch (e: Exception) {
        rawQrCode
    }
}
