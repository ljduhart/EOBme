package app.eob.me.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.ui.components.DocumentScannerOverlay
import app.eob.me.util.DocumentEdgeAnalyzer
import app.eob.me.viewmodel.EobViewModel
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FocusReticleBlue = Color(0xFF2498EA)

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraCaptureScreen(
    language: AppLanguage,
    eobViewModel: EobViewModel,
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraScanState by eobViewModel.cameraScanState.collectAsStateWithLifecycle()

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var statusMessage by remember { mutableStateOf("") }
    var isCameraReady by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var tapFocusState by remember { mutableStateOf<TapFocusState?>(null) }
    val reticleScale = remember { Animatable(2.0f) }
    val reticleAlpha = remember { Animatable(1.0f) }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            statusMessage = EobStrings.t(language, "cameraPermissionRequired")
        }
    }

    LaunchedEffect(Unit) {
        eobViewModel.clearCameraScanState()
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            eobViewModel.clearCameraScanState()
            analysisExecutor.shutdown()
        }
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
                reticleAlpha.animateTo(0f, animationSpec = tween(250))
            }
        }
    }

    LaunchedEffect(tapFocusState) {
        val state = tapFocusState ?: return@LaunchedEffect
        delay(500L)
        if (tapFocusState?.triggerTime == state.triggerTime) {
            tapFocusState = null
        }
    }

    LaunchedEffect(isCameraReady, boundCamera) {
        if (!isCameraReady) return@LaunchedEffect
        while (isActive) {
            focusAtCenter(previewView, boundCamera)
            delay(3_000)
        }
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val edgeAnalyzer = DocumentEdgeAnalyzer { bounds ->
                mainExecutor.execute {
                    eobViewModel.updateDocumentBounds(bounds)
                }
            }
            val listener = Runnable {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = buildPreviewUseCase(previewView)
                    val capture = buildImageCaptureUseCase(previewView)
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        .also { it.setAnalyzer(analysisExecutor, edgeAnalyzer) }

                    val selector = availableCameraSelector(cameraProvider)
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        capture,
                        analysis
                    )
                    boundCamera = camera
                    imageCapture = capture
                    isCameraReady = true
                    statusMessage = ""
                }.onFailure { error ->
                    boundCamera = null
                    isCameraReady = false
                    imageCapture = null
                    statusMessage = error.localizedMessage ?: "Unable to open camera. Please try again."
                }
            }
            cameraProviderFuture.addListener(listener, mainExecutor)
            onDispose {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            tapFocusState = TapFocusState(offset, System.currentTimeMillis())
                            focusAtPoint(previewView, boundCamera, offset)
                        }
                    }
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                DocumentScannerOverlay(
                    documentBounds = cameraScanState.documentBounds,
                    isDocumentDetected = cameraScanState.isDocumentDetected,
                    modifier = Modifier.fillMaxSize()
                )
                tapFocusState?.let { state ->
                    if (System.currentTimeMillis() - state.triggerTime < 500L) {
                        TapFocusReticleOverlay(
                            state = state,
                            scale = reticleScale.value,
                            alpha = reticleAlpha.value,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cameraScanState.scannerHint,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (statusMessage.isNotBlank()) {
                    Text(statusMessage, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        val capture = imageCapture
                        if (capture == null || !isCameraReady) {
                            statusMessage = "Camera is still starting. Please wait a moment."
                        } else {
                            isCapturing = true
                            scope.launch {
                                captureImage(
                                    context = context,
                                    imageCapture = capture,
                                    onImageCaptured = { uri ->
                                        isCapturing = false
                                        eobViewModel.clearCameraScanState()
                                        onImageCaptured(uri)
                                    },
                                    onError = { message ->
                                        isCapturing = false
                                        statusMessage = message
                                    }
                                )
                            }
                        }
                    },
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

data class TapFocusState(val offset: Offset, val triggerTime: Long)

@OptIn(ExperimentalCamera2Interop::class)
private fun buildPreviewUseCase(previewView: PreviewView): Preview {
    val builder = Preview.Builder()
    Camera2Interop.Extender(builder).setCaptureRequestOption(
        CaptureRequest.CONTROL_AF_MODE,
        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
    )
    return builder.build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
}

@OptIn(ExperimentalCamera2Interop::class)
private fun buildImageCaptureUseCase(previewView: PreviewView): ImageCapture {
    val builder = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .setJpegQuality(92)
        .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
    Camera2Interop.Extender(builder).setCaptureRequestOption(
        CaptureRequest.CONTROL_AF_MODE,
        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
    )
    return builder.build()
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
        val reticleColor = FocusReticleBlue.copy(alpha = alpha)

        translate(center.x, center.y) {
            scale(scale) {
                drawCircle(color = reticleColor, radius = radius, style = Stroke(width = strokeWidth))
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

private fun focusAtCenter(previewView: PreviewView, camera: Camera?) {
    val width = previewView.width.takeIf { it > 0 } ?: return
    val height = previewView.height.takeIf { it > 0 } ?: return
    focusAtPoint(previewView, camera, Offset(width / 2f, height / 2f))
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

private suspend fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val file = File(context.cacheDir, "eob_camera_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    withContext(Dispatchers.Main) {
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
}
