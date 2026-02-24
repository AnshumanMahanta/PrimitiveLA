package com.example.primitivela

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    onIdScanned: (String) -> Unit,
    onCancel: () -> Unit
) {
    // val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lastScannedValue by remember { mutableStateOf<String?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isPaused) {
                            processImageProxy(scanner, imageProxy) { result ->
                                lastScannedValue = result
                                isPaused = true
                            }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("Scanner", "Binding failed", e)
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Modern Viewfinder Overlay
        ScannerOverlay()

        // 3. Result Dialog (When Scanned)
        if (isPaused && lastScannedValue != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Scanned Data", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(
                            text = lastScannedValue!!,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                lastScannedValue = null
                                isPaused = false
                            }) {
                                Text("Retry")
                            }
                            Button(onClick = {
                                onIdScanned(lastScannedValue!!)
                                lastScannedValue = null
                                isPaused = false
                            }) {
                                Text("Accept")
                            }
                        }
                    }
                }
            }
        }

        // 4. Modern Back Button (Center Bottom)
        Button(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .height(56.dp)
                .width(160.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(
                "BACK",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
fun ScannerOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Frame Brackets
        Canvas(modifier = Modifier.size(260.dp)) {
            val strokeWidth = 4.dp.toPx()
            val cornerSize = 40.dp.toPx()
            val color = Color.White

            // Top Left
            drawLine(color, Offset(0f, 0f), Offset(cornerSize, 0f), strokeWidth)
            drawLine(color, Offset(0f, 0f), Offset(0f, cornerSize), strokeWidth)

            // Top Right
            drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerSize, 0f), strokeWidth)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerSize), strokeWidth)

            // Bottom Left
            drawLine(color, Offset(0f, size.height), Offset(cornerSize, size.height), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerSize), strokeWidth)

            // Bottom Right
            drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerSize, size.height), strokeWidth)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerSize), strokeWidth)
        }

        Text(
            text = "Align Barcode within frame",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.Center).padding(top = 320.dp)
        )
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onResult: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    barcodes.firstOrNull()?.rawValue?.let {
                        if (it.isNotBlank()) onResult(it)
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