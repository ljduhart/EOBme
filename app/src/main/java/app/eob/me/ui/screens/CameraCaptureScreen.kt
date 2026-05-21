package app.eob.me.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun CameraCaptureScreen(
    language: AppLanguage,
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(92)
            .build()
    }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var statusMessage by remember { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) statusMessage = EobStrings.t(language, "cameraPermissionRequired")
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
            var camera: Camera? = null
            val focusHandler = Handler(Looper.getMainLooper())
            var focusRunnable: Runnable? = null
            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                focusRunnable = autofocusRunnable(previewView, camera).also { focusHandler.post(it) }
            }
            cameraProviderFuture.addListener(listener, executor)
            onDispose {
                focusRunnable?.let { focusHandler.removeCallbacks(it) }
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            DocumentGuideOverlay()
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (statusMessage.isNotBlank()) {
                    Text(statusMessage, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        captureImage(context, imageCapture, onImageCaptured) { statusMessage = it }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(EobStrings.t(language, "scanBill"))
                }
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text(EobStrings.t(language, "close"))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(statusMessage.ifBlank { EobStrings.t(language, "cameraPermissionRequired") })
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(EobStrings.t(language, "scanWithCamera"))
                }
            }
        }
    }
}

@Composable
private fun DocumentGuideOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val marginX = size.width * 0.08f
        val marginY = size.height * 0.16f
        val rectSize = Size(size.width - marginX * 2, size.height - marginY * 2)
        val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(marginX, marginY),
            size = rectSize,
            cornerRadius = CornerRadius(20.dp.toPx()),
            style = stroke
        )
        drawRoundRect(
            color = Color(0xFF0B3D91).copy(alpha = 0.30f),
            topLeft = Offset(marginX, marginY),
            size = rectSize,
            cornerRadius = CornerRadius(20.dp.toPx()),
            style = Stroke(width = 12.dp.toPx())
        )
    }
}

private fun autofocusRunnable(previewView: PreviewView, camera: Camera?): Runnable {
    return object : Runnable {
        override fun run() {
            val currentCamera = camera ?: return
            val width = previewView.width.takeIf { it > 0 } ?: return
            val height = previewView.height.takeIf { it > 0 } ?: return
            val point = previewView.meteringPointFactory.createPoint(width / 2f, height / 2f)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(4, TimeUnit.SECONDS)
                .build()
            currentCamera.cameraControl.startFocusAndMetering(action)
            previewView.postDelayed(this, 3500)
        }
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val file = File(context.cacheDir, "eob_camera_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onImageCaptured(Uri.fromFile(file))
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.localizedMessage ?: "Camera capture failed")
            }
        }
    )
}
