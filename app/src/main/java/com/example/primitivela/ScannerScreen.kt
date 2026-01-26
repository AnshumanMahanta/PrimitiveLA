package com.example.primitivela // <--- ADD THIS LINE

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.camera.core.ImageAnalysis
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    onIdScanned: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // State to hold the result and pause scanning
    var lastScannedValue by remember { mutableStateOf<String?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Camera Viewfinder
        if (!isPaused) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val scanner = BarcodeScanning.getClient()
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(1) // 1 is the internal value for "Keep only instant image"
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(scanner, imageProxy) { result ->
                                if (result.isNotEmpty() && !isPaused) {
                                    lastScannedValue = result
                                    isPaused = true // Pause scanning once a code is found
                                }
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
        }

        // 2. Overlay UI (Confirmation Buttons)
        if (isPaused && lastScannedValue != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Scanned ID:", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = lastScannedValue!!,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // RETRY: Just unpause the camera
                            OutlinedButton(onClick = {
                                lastScannedValue = null
                                isPaused = false
                            }) {
                                Text("Retry")
                            }
                            // NEXT: Save and then unpause
                            Button(onClick = {
                                onIdScanned(lastScannedValue!!)
                                lastScannedValue = null
                                isPaused = false
                            }) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }

        // Close button to go back to Dashboard
        IconButton(
            onClick = onCancel,
            modifier = Modifier.padding(top = 40.dp, start = 16.dp)
        ) {
            Text("Back", color = Color.White)
        }
    }
}

// 3. Image Analysis Logic (The "Brain")
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
                for (barcode in barcodes) {
                    barcode.rawValue?.let { onResult(it) }
                }
            }
            .addOnCompleteListener {
                imageProxy.close() // ALWAYS close the frame to prevent freezing
            }
    } else {
        imageProxy.close()
    }
}