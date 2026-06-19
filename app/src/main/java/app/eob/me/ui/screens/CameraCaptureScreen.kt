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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import app.eob.me.data.CameraScanDocumentType
import app.eob.me.data.EobStrings
import app.eob.me.data.ImageCompressionLevel
import app.eob.me.scanner.DocumentCorners
import app.eob.me.scanner.ImageScaleMode
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.viewmodel.CameraCapturePhase
import app.eob.me.viewmodel.CameraCaptureViewModel
import app.eob.me.viewmodel.CameraFlashMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.concurrent.TimeUnit

private val ScannerBackdrop = Color(0xFF000000)
private val ScannerOverlayScrim = Color(0x66000000)
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
    selectedScanType: CameraScanDocumentType = CameraScanDocumentType.Eob,
    onScanTypeSelected: (CameraScanDocumentType) -> Unit = {},
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScannerBackdrop)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                    }

                    CameraTopUtilityBar(
                        language = language,
                        flashMode = uiState.flashMode,
                        onClose = onClose,
                        onToggleFlash = {
                            viewModel.cycleFlashMode()
                            applyTorch(boundCamera, viewModel.uiState.value.flashMode)
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .fillMaxWidth()
                    )

                    if (uiState.motionBlurWarning) {
                        MotionBlurBanner(
                            language = language,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 64.dp)
                        )
                    } else if (isCameraReady && !uiState.edgesStable) {
                        BordersNotFoundBanner(
                            language = language,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                        )
                    }

                    if (uiState.statusMessage.isNotBlank()) {
                        Text(
                            text = uiState.statusMessage,
                            color = MotionWarningColor,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 112.dp, start = 16.dp, end = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    CameraScanDock(
                        language = language,
                        selectedScanType = selectedScanType,
                        onScanTypeSelected = onScanTypeSelected,
                        lastCaptureThumbnail = uiState.lastCaptureThumbnail,
                        autoCaptureActive = uiState.autoCaptureActive,
                        isCapturing = uiState.isCapturing,
                        isCameraReady = isCameraReady,
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
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 28.dp)
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
private fun CameraTopUtilityBar(
    language: AppLanguage,
    flashMode: CameraFlashMode,
    onClose: () -> Unit,
    onToggleFlash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = EobStrings.t(language, "close"),
                tint = Color.White
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = ScannerOverlayScrim,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = flashLabel(language, flashMode),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            IconButton(onClick = onToggleFlash) {
                Icon(
                    imageVector = if (flashMode == CameraFlashMode.OFF) {
                        Icons.Rounded.FlashOff
                    } else {
                        Icons.Rounded.FlashOn
                    },
                    contentDescription = flashLabel(language, flashMode),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraScanDock(
    language: AppLanguage,
    selectedScanType: CameraScanDocumentType,
    onScanTypeSelected: (CameraScanDocumentType) -> Unit,
    lastCaptureThumbnail: Bitmap?,
    autoCaptureActive: Boolean,
    isCapturing: Boolean,
    isCameraReady: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        CameraScanTypeSelector(
            language = language,
            selectedScanType = selectedScanType,
            onScanTypeSelected = onScanTypeSelected
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            LastCaptureThumbnail(
                thumbnail = lastCaptureThumbnail,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(28.dp))
            CaptureButton(
                enabled = isCameraReady && !isCapturing,
                autoCaptureActive = autoCaptureActive,
                onClick = onCapture
            )
            Spacer(modifier = Modifier.width(84.dp))
        }
    }
}

@Composable
private fun CameraScanTypeSelector(
    language: AppLanguage,
    selectedScanType: CameraScanDocumentType,
    onScanTypeSelected: (CameraScanDocumentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipScrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(chipScrollState)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraScanDocumentType.entries.forEach { type ->
            val selected = type == selectedScanType
            FilterChip(
                selected = selected,
                onClick = { onScanTypeSelected(type) },
                label = {
                    Text(
                        text = scanTypeLabel(language, type),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White,
                    selectedLabelColor = Color.Black,
                    containerColor = ScannerOverlayScrim,
                    labelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun LastCaptureThumbnail(
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = ScannerOverlayScrim,
        shadowElevation = 2.dp
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "—",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun BordersNotFoundBanner(language: AppLanguage, modifier: Modifier = Modifier) {
    Surface(
        color = MotionWarningColor.copy(alpha = 0.92f),
        modifier = modifier,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = EobStrings.t(language, "cameraBordersNotFound"),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun CaptureButton(
    enabled: Boolean,
    autoCaptureActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(76.dp)
            .background(Color.Transparent, CircleShape)
            .border(
                width = 4.dp,
                color = if (autoCaptureActive) CaptureRingAuto else CaptureRingIdle,
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(
                    color = Color.White.copy(alpha = if (enabled) 0.96f else 0.35f),
                    shape = CircleShape
                )
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
        Surface(color = ScannerOverlayScrim, modifier = Modifier.weight(0.15f).fillMaxWidth()) {
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

private fun scanTypeLabel(language: AppLanguage, type: CameraScanDocumentType): String = when (type) {
    CameraScanDocumentType.Eob -> EobStrings.t(language, "cameraScanTypeEob")
    CameraScanDocumentType.Receipt -> EobStrings.t(language, "cameraScanTypeReceipt")
}

private fun flashLabel(language: AppLanguage, mode: CameraFlashMode): String = when (mode) {
    CameraFlashMode.AUTO -> EobStrings.t(language, "cameraFlashAuto")
    CameraFlashMode.ON -> EobStrings.t(language, "cameraFlashOn")
    CameraFlashMode.OFF -> EobStrings.t(language, "cameraFlashOff")
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
