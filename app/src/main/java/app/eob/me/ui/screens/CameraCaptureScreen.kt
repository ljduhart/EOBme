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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.ui.components.CameraScanningOverlay
import app.eob.me.util.DocumentScanCrop
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF2498EA)
private const val TapFocusReticleActiveMs = 500L

data class TapFocusState(val offset: Offset, val triggerTime: Long)

@Composable
fun CameraCaptureScreen(
    language: AppLanguage,
    autoCropEnabled: Boolean = true,
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
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var tapFocusState by remember { mutableStateOf<TapFocusState?>(null) }
    val reticleScale = remember { Animatable(2.0f) }
    val reticleAlpha = remember { Animatable(1.0f) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) statusMessage = EobStrings.t(language, "cameraPermissionRequired")
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(tapFocusState) {
        tapFocusState?.let {
            reticleScale.snapTo(2.0f)
            reticleAlpha.snapTo(1.0f)
            launch {
                reticleScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                delay(250)
                reticleAlpha.animateTo(
                    targetValue = 0.0f,
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                )
            }
        }
    }

    LaunchedEffect(tapFocusState) {
        val state = tapFocusState ?: return@LaunchedEffect
        delay(TapFocusReticleActiveMs)
        if (tapFocusState?.triggerTime == state.triggerTime) {
            tapFocusState = null
        }
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
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
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        capture
                    )
                    boundCamera = camera
                    imageCapture = capture
                    isCameraReady = true
                    statusMessage = ""
                    focusRunnable = autofocusRunnable(previewView, camera).also { focusHandler.postDelayed(it, 400) }
                }.onFailure { error ->
                    boundCamera = null
                    isCameraReady = false
                    imageCapture = null
                    statusMessage = error.localizedMessage
                        ?: EobStrings.t(language, "cameraOpenFailed")
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
                boundCamera = null
                imageCapture = null
                isCameraReady = false
            }
        }
    }

    if (hasPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Block 1: The Secure Viewfinder & Gesture Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            tapFocusState = TapFocusState(offset, System.currentTimeMillis())
                            focusAtPoint(
                                previewView = previewView,
                                camera = boundCamera,
                                offset = offset
                            )
                        }
                    }
            ) {
                CameraViewfinderStream(
                    previewView = previewView,
                    modifier = Modifier.fillMaxSize()
                )
                CameraScanningOverlay(modifier = Modifier.fillMaxSize())
                tapFocusState?.let { state ->
                    if (System.currentTimeMillis() - state.triggerTime < TapFocusReticleActiveMs) {
                        TapFocusReticleOverlay(
                            state = state,
                            scale = reticleScale.value,
                            alpha = reticleAlpha.value,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            // Block 2: The Interactive HUD Control Interface Layer (Sits on top)
            CameraControlHudElements(
                language = language,
                statusMessage = statusMessage,
                isCameraReady = isCameraReady,
                isCapturing = isCapturing,
                onClose = onClose,
                onRequestCapture = {
                    val capture = imageCapture
                    if (capture == null || !isCameraReady) {
                        statusMessage = EobStrings.t(language, "cameraStarting")
                    } else {
                        isCapturing = true
                        captureImage(
                            context = context,
                            language = language,
                            imageCapture = capture,
                            autoCropEnabled = autoCropEnabled,
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
            Text(
                if (isCapturing) {
                    EobStrings.t(language, "capturing")
                } else {
                    EobStrings.t(language, "scanBill")
                }
            )
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

@Composable
private fun TapFocusReticleOverlay(
    state: TapFocusState,
    scale: Float,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = state.offset
        val radius = 36.dp.toPx()
        val strokeWidth = 2.dp.toPx()
        val tickLength = 12.dp.toPx()
        val reticleColor = BrandBlue.copy(alpha = alpha)

        translate(center.x, center.y) {
            scale(scale) {
                drawCircle(
                    color = reticleColor,
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = reticleColor,
                    start = Offset(-tickLength, 0f),
                    end = Offset(-radius + 6.dp.toPx(), 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = reticleColor,
                    start = Offset(radius - 6.dp.toPx(), 0f),
                    end = Offset(tickLength, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = reticleColor,
                    start = Offset(0f, -tickLength),
                    end = Offset(0f, -radius + 6.dp.toPx()),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = reticleColor,
                    start = Offset(0f, radius - 6.dp.toPx()),
                    end = Offset(0f, tickLength),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

private fun focusAtPoint(previewView: PreviewView, camera: Camera?, offset: Offset) {
    val currentCamera = camera ?: return
    if (previewView.width <= 0 || previewView.height <= 0) return
    val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
        .setAutoCancelDuration(4, TimeUnit.SECONDS)
        .build()
    currentCamera.cameraControl.startFocusAndMetering(action)
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
    language: AppLanguage,
    imageCapture: ImageCapture,
    autoCropEnabled: Boolean,
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
                if (autoCropEnabled) {
                    runCatching {
                        val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching
                        val cropped = DocumentScanCrop.applyGuideCrop(decoded)
                        FileOutputStream(file).use { output ->
                            cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, output)
                        }
                        if (cropped !== decoded) decoded.recycle()
                        cropped.recycle()
                    }
                }
                onImageCaptured(Uri.fromFile(file))
            }

            override fun onError(exception: ImageCaptureException) {
                onError(
                    exception.localizedMessage ?: EobStrings.t(language, "cameraCaptureFailed")
                )
            }
        }
    )
}
