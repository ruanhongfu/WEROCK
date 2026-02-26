package com.werock.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.werock.demo.ui.theme.WEROCKTheme
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private val cameraPermissionGranted = mutableStateOf(false)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            cameraPermissionGranted.value = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermissionGranted.value =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        enableEdgeToEdge()
        setContent {
            WEROCKTheme {
                CameraScreen(
                    hasCameraPermission = cameraPermissionGranted.value,
                    onRequestPermission = {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraScreen(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    if (hasCameraPermission) {
        CameraPreview(modifier = Modifier.fillMaxSize())
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onRequestPermission) {
                Text("Grant camera permission")
            }
        }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var minZoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { view ->
                    previewView = view
                }
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                ) {
                    Text(if (lensFacing == CameraSelector.LENS_FACING_BACK) "Switch to Front" else "Switch to Rear")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(if (lensFacing == CameraSelector.LENS_FACING_BACK) "Rear Camera" else "Front Camera")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Zoom ${"%.2f".format(zoomRatio)}x")
            Slider(
                value = zoomRatio,
                onValueChange = { value ->
                    zoomRatio = value
                    boundCamera?.cameraControl?.setZoomRatio(value)
                },
                valueRange = minZoomRatio..max(maxZoomRatio, minZoomRatio + 0.01f)
            )
        }
    }

    DisposableEffect(lifecycleOwner, cameraProviderFuture, previewView, lensFacing) {
        val executor = ContextCompat.getMainExecutor(context)
        var cameraProvider: ProcessCameraProvider? = null

        val listener = Runnable {
            val view = previewView ?: return@Runnable
            cameraProvider = cameraProviderFuture.get()
            val previewUseCase = CameraPreviewUseCase.Builder().build().also { preview ->
                preview.setSurfaceProvider(view.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            cameraProvider?.unbindAll()
            boundCamera = cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase)
            val zoomState = boundCamera?.cameraInfo?.zoomState?.value
            if (zoomState != null) {
                minZoomRatio = zoomState.minZoomRatio
                maxZoomRatio = zoomState.maxZoomRatio
                zoomRatio = zoomRatio.coerceIn(minZoomRatio, maxZoomRatio)
                boundCamera?.cameraControl?.setZoomRatio(zoomRatio)
            }
        }
        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            boundCamera = null
            cameraProvider?.unbindAll()
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    WEROCKTheme {
        CameraScreen(hasCameraPermission = false, onRequestPermission = {})
    }
}
