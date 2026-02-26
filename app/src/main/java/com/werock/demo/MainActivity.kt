package com.werock.demo

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.werock.demo.ui.theme.WEROCKTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private val cameraPermissionGranted = mutableStateOf(false)
    private val mediaPermissionGranted = mutableStateOf(false)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            cameraPermissionGranted.value = hasPermission(Manifest.permission.CAMERA)
            mediaPermissionGranted.value = hasMediaReadPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermissionGranted.value = hasPermission(Manifest.permission.CAMERA)
        mediaPermissionGranted.value = hasMediaReadPermission()

        enableEdgeToEdge()
        setContent {
            WEROCKTheme {
                CameraScreen(
                    hasCameraPermission = cameraPermissionGranted.value,
                    hasMediaPermission = mediaPermissionGranted.value,
                    onRequestPermissions = {
                        requestPermissionLauncher.launch(requiredRuntimePermissions())
                    }
                )
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasMediaReadPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.READ_MEDIA_IMAGES) &&
                hasPermission(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun requiredRuntimePermissions(): Array<String> {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.READ_MEDIA_IMAGES
            permissions += Manifest.permission.READ_MEDIA_VIDEO
        } else {
            permissions += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        return permissions.toTypedArray()
    }
}

@Composable
private fun CameraScreen(
    hasCameraPermission: Boolean,
    hasMediaPermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    if (hasCameraPermission) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            hasMediaPermission = hasMediaPermission,
            onRequestPermissions = onRequestPermissions
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onRequestPermissions) {
                Text("Grant camera and media permissions")
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    hasMediaPermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var minZoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var videoCaptureUseCase by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStatus by remember { mutableStateOf("Idle") }
    var storageInfo by remember { mutableStateOf<StorageInfo?>(null) }
    var storageRefreshKey by remember { mutableStateOf(0) }

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
                    enabled = !isRecording,
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
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = videoCaptureUseCase != null,
                    onClick = {
                        if (isRecording) {
                            activeRecording?.stop()
                        } else {
                            val capture = videoCaptureUseCase ?: return@Button
                            val fileName = "WEROCK_${System.currentTimeMillis()}"
                            val contentValues = ContentValues().apply {
                                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                put(
                                    MediaStore.Video.Media.RELATIVE_PATH,
                                    "${Environment.DIRECTORY_DCIM}/Camera"
                                )
                            }
                            val outputOptions = MediaStoreOutputOptions.Builder(
                                context.contentResolver,
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            )
                                .setContentValues(contentValues)
                                .build()
                            activeRecording = capture.output
                                .prepareRecording(context, outputOptions)
                                .start(ContextCompat.getMainExecutor(context)) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> {
                                            isRecording = true
                                            recordingStatus = "Recording..."
                                        }

                                        is VideoRecordEvent.Finalize -> {
                                            isRecording = false
                                            activeRecording = null
                                            recordingStatus = if (event.hasError()) {
                                                "Record failed"
                                            } else {
                                                "Saved to DCIM/Camera"
                                            }
                                            storageRefreshKey += 1
                                        }
                                    }
                                }
                        }
                    }
                ) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(recordingStatus)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Text("Storage", style = MaterialTheme.typography.titleSmall, color = Color.White)
            val info = storageInfo
            if (info == null) {
                Text("Loading...", color = Color.White)
            } else {
                Text("Total: ${formatBytes(info.totalBytes)}", color = Color.White)
                Text("Free: ${formatBytes(info.freeBytes)}", color = Color.White)
                val cameraSizeText = info.cameraFolderBytes?.let { formatBytes(it) } ?: "Permission needed"
                Text("Camera folder: $cameraSizeText", color = Color.White)
            }
            if (!hasMediaPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRequestPermissions) {
                    Text("Allow media access")
                }
            }
        }
    }

    LaunchedEffect(hasMediaPermission, storageRefreshKey) {
        while (true) {
            storageInfo = withContext(Dispatchers.IO) {
                getStorageInfo(context, hasMediaPermission)
            }
            delay(10_000)
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
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.HD,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )
                )
                .build()
            val createdVideoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            cameraProvider?.unbindAll()
            boundCamera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                createdVideoCapture
            )
            videoCaptureUseCase = createdVideoCapture
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
            activeRecording?.stop()
            activeRecording = null
            isRecording = false
            videoCaptureUseCase = null
            boundCamera = null
            cameraProvider?.unbindAll()
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    WEROCKTheme {
        CameraScreen(
            hasCameraPermission = false,
            hasMediaPermission = false,
            onRequestPermissions = {}
        )
    }
}

private data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val cameraFolderBytes: Long?
)

private fun getStorageInfo(context: Context, hasMediaPermission: Boolean): StorageInfo {
    val statFs = StatFs(context.filesDir.absolutePath)
    val totalBytes = statFs.totalBytes
    val freeBytes = statFs.availableBytes
    val cameraBytes = if (hasMediaPermission) {
        getCameraFolderSize(context)
    } else {
        null
    }
    return StorageInfo(
        totalBytes = totalBytes,
        freeBytes = freeBytes,
        cameraFolderBytes = cameraBytes
    )
}

private fun getCameraFolderSize(context: Context): Long {
    val resolver = context.contentResolver
    val externalUri: Uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(MediaStore.MediaColumns.SIZE)
    val selection = (
        "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR " +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
        )
    val selectionArgs = arrayOf(
        "${Environment.DIRECTORY_DCIM}/Camera/",
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
    )
    resolver.query(
        externalUri,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        var totalSize = 0L
        while (cursor.moveToNext()) {
            totalSize += cursor.getLong(sizeIndex)
        }
        return totalSize
    }
    return 0L
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gb -> String.format(Locale.US, "%.2f GB", value / gb)
        value >= mb -> String.format(Locale.US, "%.1f MB", value / mb)
        value >= kb -> String.format(Locale.US, "%.1f KB", value / kb)
        else -> "$bytes B"
    }
}
