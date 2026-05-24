package app.eob.me.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.ui.components.CameraScanningOverlay
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
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var statusMessage by remember { mutableStateOf("") }
    var isCameraReady by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
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
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setJpegQuality(92)
                        .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                        .build()
                    val selector = availableCameraSelector(cameraProvider)
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        capture
                    )
                    imageCapture = capture
                    isCameraReady = true
                    statusMessage = ""
                    focusRunnable = autofocusRunnable(previewView, camera).also { focusHandler.postDelayed(it, 400) }
                }.onFailure { error ->
                    isCameraReady = false
                    imageCapture = null
                    statusMessage = error.localizedMessage ?: "Unable to open camera. Please try again."
                }
            }
            cameraProviderFuture.addListener(listener, executor)
            onDispose {
                focusRunnable?.let { focusHandler.removeCallbacks(it) }
                runCatching {
                    if (cameraProviderFuture.isDone) {
                        cameraProviderFuture.get().unbindAll()
                    }
                }
                imageCapture = null
                isCameraReady = false
            }
        }
    }

    if (hasPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraViewfinderStream(
                previewView = previewView,
                modifier = Modifier.fillMaxSize()
            )
            CameraScanningOverlay(modifier = Modifier.fillMaxSize())
            CameraControlHudElements(
                language = language,
                statusMessage = statusMessage,
                isCameraReady = isCameraReady,
                isCapturing = isCapturing,
                onClose = onClose,
                onRequestCapture = {
                    val capture = imageCapture
                    if (capture == null || !isCameraReady) {
                        statusMessage = "Camera is still starting. Please wait a moment."
                    } else {
                        isCapturing = true
                        captureImage(
                            context = context,
                            imageCapture = capture,
                            onImageCaptured = {
                                isCapturing = false
                                onImageCaptured(it)
                            },
                            onError = {
                                isCapturing = false
                                statusMessage = it
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        CameraPermissionPrompt(
            language = language,
            statusMessage = statusMessage,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CameraViewfinderStream(
    previewView: PreviewView,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun CameraControlHudElements(
    language: AppLanguage,
    statusMessage: String,
    isCameraReady: Boolean,
    isCapturing: Boolean,
    onClose: () -> Unit,
    onRequestCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(20.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (statusMessage.isNotBlank()) {
            Text(statusMessage, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onRequestCapture,
            enabled = isCameraReady && !isCapturing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isCapturing) "Capturing..." else EobStrings.t(language, "scanBill"))
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "close"))
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    language: AppLanguage,
    statusMessage: String,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(statusMessage.ifBlank { EobStrings.t(language, "cameraPermissionRequired") })
        Button(onClick = onRequestPermission) {
            Text(EobStrings.t(language, "scanWithCamera"))
        }
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

private fun availableCameraSelector(cameraProvider: ProcessCameraProvider): CameraSelector {
    return try {
        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
    } catch (_: CameraInfoUnavailableException) {
        CameraSelector.DEFAULT_BACK_CAMERA
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
