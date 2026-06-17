package app.eob.me.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.ImageCompressionLevel
import app.eob.me.scanner.DocumentCorners
import app.eob.me.scanner.ImageScaleMode
import app.eob.me.scanner.ScanFilterMode
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.viewmodel.CameraCapturePhase
import app.eob.me.viewmodel.CameraCaptureViewModel
import app.eob.me.viewmodel.CameraFlashMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.concurrent.TimeUnit

private val ScannerBackdrop = Color(0xFF0A0D14)
private val ScannerControlBar = Color(0xFF121722)
private val EdgeStableColor = Color(0xFF3DDC97)
private val EdgeSearchingColor = Color(0xFFFFC857)
private val MotionWarningColor = Color(0xFFFF6B6B)
private val CaptureRingIdle = Color(0xFFE8ECF5)
private val CaptureRingAuto = Color(0xFF3DDC97)

@Composable
fun CameraCaptureScreen(
    language: AppLanguage,
    autoCropEnabled: Boolean = true,
    imageCompression: ImageCompressionLevel = ImageCompressionLevel.Medium,
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    viewModel: CameraCaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isCameraReady by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) viewModel.onCaptureFailed(EobStrings.t(language, "cameraPermissionRequired"))
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        viewModel.startMotionMonitoring()
        onDispose { viewModel.stopMotionMonitoring() }
    }

    LaunchedEffect(isCameraReady) {
        while (isActive && isCameraReady && uiState.phase == CameraCapturePhase.LIVE_PREVIEW) {
            viewModel.refreshMotionState()
            previewView.bitmap?.let { frame ->
                viewModel.onPreviewFrame(frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false))
            }
            delay(180L)
        }
    }

    LaunchedEffect(uiState.autoCaptureActive, isCameraReady) {
        if (isCameraReady && viewModel.shouldTriggerAutoCapture()) {
            viewModel.markAutoCaptureTriggered()
            viewModel.onManualCaptureRequested()
            captureImage(
                context = context,
                language = language,
                imageCapture = imageCapture,
                viewModel = viewModel,
                autoCropEnabled = autoCropEnabled,
                imageCompression = imageCompression,
                onImageCaptured = onImageCaptured
            )
        }
    }

    DisposableEffect(hasPermission, uiState.flashMode) {
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
                        .setJpegQuality(imageCompression.jpegQuality)
                        .setFlashMode(flashModeToImageCapture(uiState.flashMode))
                        .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                        .build()
                    val selector = availableCameraSelector(cameraProvider)
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                    boundCamera = camera
                    imageCapture = capture
                    applyTorch(camera, uiState.flashMode)
                    isCameraReady = true
                    focusRunnable = autofocusRunnable(previewView, camera).also { focusHandler.postDelayed(it, 400) }
                }.onFailure { error ->
                    boundCamera = null
                    isCameraReady = false
                    imageCapture = null
                    viewModel.onCaptureFailed(
                        error.localizedMessage ?: EobStrings.t(language, "cameraOpenFailed")
                    )
                }
            }
            cameraProviderFuture.addListener(listener, executor)
            onDispose {
                focusRunnable?.let { focusHandler.removeCallbacks(it) }
                runCatching {
                    if (cameraProviderFuture.isDone) cameraProviderFuture.get().unbindAll()
                }
                boundCamera = null
                imageCapture = null
                isCameraReady = false
            }
        }
    }

    when (uiState.phase) {
        CameraCapturePhase.POST_CAPTURE_ADJUST -> {
            MagneticCropScreen(
                language = language,
                bitmap = uiState.capturedBitmap,
                corners = uiState.adjustableCorners,
                statusMessage = uiState.statusMessage,
                onCornerMoved = viewModel::updateAdjustableCorner,
                onSnapCorners = viewModel::snapCornersMagnetically,
                onRetake = viewModel::resetToLivePreview,
                onConfirm = {
                    viewModel.confirmCapture(
                        compression = imageCompression,
                        onComplete = onImageCaptured,
                        onError = viewModel::onExportFailed
                    )
                }
            )
        }
        CameraCapturePhase.PROCESSING -> {
            ProcessingScreen(language = language)
        }
        CameraCapturePhase.LIVE_PREVIEW -> {
            if (hasPermission) {
                Column(modifier = Modifier.fillMaxSize().background(ScannerBackdrop)) {
                    Box(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    focusAtPoint(previewView, boundCamera, offset)
                                }
                            }
                    ) {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                        DocumentEdgeOverlay(
                            corners = uiState.detectedCorners,
                            analysisWidth = uiState.analysisWidth,
                            analysisHeight = uiState.analysisHeight,
                            edgesStable = uiState.edgesStable,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (uiState.motionBlurWarning) {
                            MotionBlurBanner(
                                language = language,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                            )
                        }
                        if (uiState.statusMessage.isNotBlank()) {
                            Text(
                                text = uiState.statusMessage,
                                color = MotionWarningColor,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 56.dp, start = 16.dp, end = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    ScannerControlBar(
                        language = language,
                        flashMode = uiState.flashMode,
                        filterMode = uiState.filterMode,
                        autoCaptureEnabled = uiState.autoCaptureEnabled,
                        autoCaptureActive = uiState.autoCaptureActive,
                        isCapturing = uiState.isCapturing,
                        isCameraReady = isCameraReady,
                        onClose = onClose,
                        onCapture = {
                            viewModel.onManualCaptureRequested()
                            captureImage(
                                context = context,
                                language = language,
                                imageCapture = imageCapture,
                                viewModel = viewModel,
                                autoCropEnabled = autoCropEnabled,
                                imageCompression = imageCompression,
                                onImageCaptured = onImageCaptured
                            )
                        },
                        onToggleFlash = {
                            viewModel.cycleFlashMode()
                            applyTorch(boundCamera, viewModel.uiState.value.flashMode)
                        },
                        onToggleFilter = viewModel::cycleFilterMode,
                        onToggleAutoCapture = viewModel::toggleAutoCapture,
                        modifier = Modifier
                            .weight(0.15f)
                            .fillMaxWidth()
                    )
                }
            } else {
                CameraPermissionPrompt(
                    language = language,
                    statusMessage = uiState.statusMessage,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ScannerControlBar(
    language: AppLanguage,
    flashMode: CameraFlashMode,
    filterMode: ScanFilterMode,
    autoCaptureEnabled: Boolean,
    autoCaptureActive: Boolean,
    isCapturing: Boolean,
    isCameraReady: Boolean,
    onClose: () -> Unit,
    onCapture: () -> Unit,
    onToggleFlash: () -> Unit,
    onToggleFilter: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = ScannerControlBar, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onClose) {
                Text(EobStrings.t(language, "close"), color = Color.White)
            }
            CaptureButton(
                enabled = isCameraReady && !isCapturing,
                autoCaptureActive = autoCaptureActive,
                onClick = onCapture
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = onToggleFlash) {
                    Text(flashLabel(language, flashMode), color = Color.White)
                }
                TextButton(onClick = onToggleFilter) {
                    Text(filterLabel(language, filterMode), color = Color.White)
                }
                TextButton(onClick = onToggleAutoCapture) {
                    Text(
                        if (autoCaptureEnabled) {
                            EobStrings.t(language, "cameraAutoCaptureOn")
                        } else {
                            EobStrings.t(language, "cameraAutoCaptureOff")
                        },
                        color = if (autoCaptureActive) CaptureRingAuto else Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureButton(
    enabled: Boolean,
    autoCaptureActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (autoCaptureActive) CaptureRingAuto else EobBrandBlue,
            disabledContainerColor = CaptureRingIdle.copy(alpha = 0.35f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(Color.White.copy(alpha = if (enabled) 0.92f else 0.35f), CircleShape)
        )
    }
}

@Composable
private fun DocumentEdgeOverlay(
    corners: DocumentCorners?,
    analysisWidth: Int,
    analysisHeight: Int,
    edgesStable: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (corners == null || analysisWidth <= 0 || analysisHeight <= 0) return@Canvas
        val mapped = corners.mapToView(
            sourceWidth = analysisWidth.toFloat(),
            sourceHeight = analysisHeight.toFloat(),
            viewWidth = size.width,
            viewHeight = size.height,
            scaleMode = ImageScaleMode.FILL
        )
        val points = mapped.toPolygonOffsets()
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
            lineTo(points[2].x, points[2].y)
            lineTo(points[3].x, points[3].y)
            close()
        }
        drawPath(
            path = path,
            color = if (edgesStable) EdgeStableColor else EdgeSearchingColor,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun MotionBlurBanner(language: AppLanguage, modifier: Modifier = Modifier) {
    Surface(
        color = MotionWarningColor.copy(alpha = 0.92f),
        modifier = modifier,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = EobStrings.t(language, "cameraMotionBlurWarning"),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MagneticCropScreen(
    language: AppLanguage,
    bitmap: Bitmap?,
    corners: DocumentCorners?,
    statusMessage: String,
    onCornerMoved: (Int, Offset) -> Unit,
    onSnapCorners: () -> Unit,
    onRetake: () -> Unit,
    onConfirm: () -> Unit
) {
    if (bitmap == null || corners == null) {
        ProcessingScreen(language = language)
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScannerBackdrop)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxWidth()
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val viewHeight = constraints.maxHeight.toFloat()
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()
            val viewCorners = corners.mapToView(
                bitmapWidth,
                bitmapHeight,
                viewWidth,
                viewHeight,
                scaleMode = ImageScaleMode.FIT
            )
            val viewToBitmap = corners.mapFromView(
                bitmapWidth,
                bitmapHeight,
                viewWidth,
                viewHeight,
                scaleMode = ImageScaleMode.FIT
            )

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(viewCorners) {
                        detectDragGestures { change, _ ->
                            val index = nearestCornerIndex(viewCorners, change.position)
                            val bitmapPoint = viewToBitmap(change.position)
                            onCornerMoved(index, Offset(bitmapPoint.x, bitmapPoint.y))
                        }
                    }
            ) {
                val points = viewCorners.toPolygonOffsets()
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    lineTo(points[1].x, points[1].y)
                    lineTo(points[2].x, points[2].y)
                    lineTo(points[3].x, points[3].y)
                    close()
                }
                drawPath(path = path, color = EdgeStableColor, style = Stroke(width = 3.dp.toPx()))
                points.forEach { point ->
                    drawCircle(color = Color.White, radius = 12.dp.toPx(), center = point)
                    drawCircle(color = EobBrandBlue, radius = 8.dp.toPx(), center = point)
                }
            }
        }
        Surface(color = ScannerControlBar, modifier = Modifier.weight(0.15f).fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = EobStrings.t(language, "cameraMagneticCropHint"),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
                if (statusMessage.isNotBlank()) {
                    Text(statusMessage, color = MotionWarningColor, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onRetake) {
                        Text(EobStrings.t(language, "cameraRetake"), color = Color.White)
                    }
                    TextButton(onClick = onSnapCorners) {
                        Text(EobStrings.t(language, "cameraSnapCorners"), color = Color.White)
                    }
                    Button(onClick = onConfirm) {
                        Text(EobStrings.t(language, "cameraUseScan"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessingScreen(language: AppLanguage) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScannerBackdrop),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = EobBrandBlue)
            Text(
                text = EobStrings.t(language, "cameraProcessingScan"),
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp)
            )
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

private fun flashLabel(language: AppLanguage, mode: CameraFlashMode): String = when (mode) {
    CameraFlashMode.AUTO -> EobStrings.t(language, "cameraFlashAuto")
    CameraFlashMode.ON -> EobStrings.t(language, "cameraFlashOn")
    CameraFlashMode.OFF -> EobStrings.t(language, "cameraFlashOff")
}

private fun filterLabel(language: AppLanguage, mode: ScanFilterMode): String = when (mode) {
    ScanFilterMode.COLOR -> EobStrings.t(language, "cameraFilterColor")
    ScanFilterMode.GRAYSCALE -> EobStrings.t(language, "cameraFilterGrayscale")
    ScanFilterMode.BLACK_WHITE -> EobStrings.t(language, "cameraFilterBlackWhite")
}

private fun flashModeToImageCapture(mode: CameraFlashMode): Int = when (mode) {
    CameraFlashMode.ON -> ImageCapture.FLASH_MODE_ON
    CameraFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    CameraFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
}

private fun applyTorch(camera: Camera?, mode: CameraFlashMode) {
    val enabled = mode == CameraFlashMode.ON
    runCatching { camera?.cameraControl?.enableTorch(enabled) }
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
    imageCapture: ImageCapture?,
    viewModel: CameraCaptureViewModel,
    autoCropEnabled: Boolean,
    imageCompression: ImageCompressionLevel,
    onImageCaptured: (Uri) -> Unit
) {
    val capture = imageCapture
    if (capture == null) {
        viewModel.onCaptureFailed(EobStrings.t(language, "cameraStarting"))
        return
    }
    val file = File(context.cacheDir, "eob_camera_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    capture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                viewModel.handleCapturedFile(
                    filePath = file.absolutePath,
                    autoCropEnabled = autoCropEnabled,
                    compression = imageCompression,
                    onComplete = onImageCaptured,
                    onError = { message -> viewModel.onCaptureFailed(message) }
                )
            }

            override fun onError(exception: ImageCaptureException) {
                viewModel.onCaptureFailed(
                    exception.localizedMessage ?: EobStrings.t(language, "cameraCaptureFailed")
                )
            }
        }
    )
}

private fun nearestCornerIndex(corners: DocumentCorners, touch: Offset): Int {
    val points = corners.toPolygonOffsets()
    return points.indices.minByOrNull { index ->
        val point = points[index]
        val dx = point.x - touch.x
        val dy = point.y - touch.y
        dx * dx + dy * dy
    } ?: 0
}
