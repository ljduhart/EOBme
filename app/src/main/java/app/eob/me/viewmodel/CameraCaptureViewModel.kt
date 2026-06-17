package app.eob.me.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.data.ImageCompressionLevel
import app.eob.me.scanner.DeviceMotionMonitor
import app.eob.me.scanner.DocumentCorners
import app.eob.me.scanner.DocumentEdgeDetector
import app.eob.me.scanner.DocumentScanProcessor
import app.eob.me.scanner.ScanFilterMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CameraFlashMode {
    AUTO,
    ON,
    OFF
}

enum class CameraCapturePhase {
    LIVE_PREVIEW,
    POST_CAPTURE_ADJUST,
    PROCESSING
}

data class CameraCaptureUiState(
    val phase: CameraCapturePhase = CameraCapturePhase.LIVE_PREVIEW,
    val flashMode: CameraFlashMode = CameraFlashMode.AUTO,
    val filterMode: ScanFilterMode = ScanFilterMode.COLOR,
    val autoCaptureEnabled: Boolean = true,
    val autoCaptureActive: Boolean = false,
    val motionBlurWarning: Boolean = false,
    val edgesStable: Boolean = false,
    val detectedCorners: DocumentCorners? = null,
    val analysisWidth: Int = 0,
    val analysisHeight: Int = 0,
    val adjustableCorners: DocumentCorners? = null,
    val capturedBitmap: Bitmap? = null,
    val isCapturing: Boolean = false,
    val statusMessage: String = "",
    val processedUri: Uri? = null
)

class CameraCaptureViewModel(application: Application) : AndroidViewModel(application) {
    private val motionMonitor = DeviceMotionMonitor(application.applicationContext)

    private val _uiState = MutableStateFlow(CameraCaptureUiState())
    val uiState: StateFlow<CameraCaptureUiState> = _uiState.asStateFlow()

    private var previousCorners: DocumentCorners? = null
    private var stableFrameCount = 0
    private var autoCaptureTriggered = false

    fun startMotionMonitoring() {
        motionMonitor.start()
    }

    fun stopMotionMonitoring() {
        motionMonitor.stop()
    }

    fun refreshMotionState() {
        val shaking = motionMonitor.isShaking
        _uiState.update { state ->
            state.copy(
                motionBlurWarning = shaking,
                autoCaptureActive = state.autoCaptureEnabled &&
                    state.edgesStable &&
                    !shaking &&
                    state.phase == CameraCapturePhase.LIVE_PREVIEW &&
                    !state.isCapturing
            )
        }
    }

    fun cycleFlashMode() {
        _uiState.update { state ->
            val next = when (state.flashMode) {
                CameraFlashMode.AUTO -> CameraFlashMode.ON
                CameraFlashMode.ON -> CameraFlashMode.OFF
                CameraFlashMode.OFF -> CameraFlashMode.AUTO
            }
            state.copy(flashMode = next)
        }
    }

    fun cycleFilterMode() {
        _uiState.update { state ->
            val next = when (state.filterMode) {
                ScanFilterMode.COLOR -> ScanFilterMode.GRAYSCALE
                ScanFilterMode.GRAYSCALE -> ScanFilterMode.BLACK_WHITE
                ScanFilterMode.BLACK_WHITE -> ScanFilterMode.COLOR
            }
            state.copy(filterMode = next)
        }
    }

    fun toggleAutoCapture() {
        autoCaptureTriggered = false
        _uiState.update { state ->
            state.copy(autoCaptureEnabled = !state.autoCaptureEnabled)
        }
        refreshMotionState()
    }

    fun onPreviewFrame(bitmap: Bitmap) {
        if (_uiState.value.phase != CameraCapturePhase.LIVE_PREVIEW) {
            bitmap.recycle()
            return
        }
        val corners = DocumentEdgeDetector.detectCorners(bitmap)
        val stable = DocumentEdgeDetector.isStable(previousCorners, corners)
        previousCorners = corners
        stableFrameCount = if (stable) stableFrameCount + 1 else 0
        val edgesStable = stableFrameCount >= STABLE_FRAME_THRESHOLD
        val shaking = motionMonitor.isShaking
        _uiState.update { state ->
            state.copy(
                detectedCorners = corners,
                analysisWidth = bitmap.width,
                analysisHeight = bitmap.height,
                edgesStable = edgesStable,
                motionBlurWarning = shaking,
                autoCaptureActive = state.autoCaptureEnabled &&
                    edgesStable &&
                    !shaking &&
                    !state.isCapturing
            )
        }
        bitmap.recycle()
    }

    fun shouldTriggerAutoCapture(): Boolean {
        val state = _uiState.value
        return !autoCaptureTriggered &&
            state.phase == CameraCapturePhase.LIVE_PREVIEW &&
            state.autoCaptureActive &&
            !state.isCapturing
    }

    fun markAutoCaptureTriggered() {
        autoCaptureTriggered = true
    }

    fun onManualCaptureRequested() {
        if (_uiState.value.isCapturing) return
        _uiState.update { it.copy(isCapturing = true, statusMessage = "") }
    }

    fun handleCapturedFile(
        filePath: String,
        autoCropEnabled: Boolean,
        compression: ImageCompressionLevel,
        onComplete: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val decoded = runCatching {
                withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(filePath) ?: error("Unable to decode captured image.")
                }
            }.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        phase = CameraCapturePhase.LIVE_PREVIEW,
                        statusMessage = error.localizedMessage.orEmpty()
                    )
                }
                onError(error.localizedMessage.orEmpty())
                return@launch
            }

            if (autoCropEnabled) {
                val detected = _uiState.value.detectedCorners
                    ?: DocumentCorners.guideFrame(decoded.width.toFloat(), decoded.height.toFloat())
                val scaled = if (_uiState.value.analysisWidth > 0 && _uiState.value.analysisHeight > 0) {
                    detected.scaleTo(
                        fromWidth = _uiState.value.analysisWidth.toFloat(),
                        fromHeight = _uiState.value.analysisHeight.toFloat(),
                        toWidth = decoded.width.toFloat(),
                        toHeight = decoded.height.toFloat()
                    )
                } else {
                    DocumentCorners.guideFrame(decoded.width.toFloat(), decoded.height.toFloat())
                }
                _uiState.update {
                    it.copy(
                        phase = CameraCapturePhase.POST_CAPTURE_ADJUST,
                        capturedBitmap = decoded,
                        adjustableCorners = scaled,
                        isCapturing = false,
                        autoCaptureActive = false
                    )
                }
                return@launch
            }

            exportBitmap(
                bitmap = decoded,
                corners = run {
                    val detected = _uiState.value.detectedCorners
                        ?: DocumentCorners.guideFrame(decoded.width.toFloat(), decoded.height.toFloat())
                    if (_uiState.value.analysisWidth > 0 && _uiState.value.analysisHeight > 0) {
                        detected.scaleTo(
                            fromWidth = _uiState.value.analysisWidth.toFloat(),
                            fromHeight = _uiState.value.analysisHeight.toFloat(),
                            toWidth = decoded.width.toFloat(),
                            toHeight = decoded.height.toFloat()
                        )
                    } else {
                        DocumentCorners.guideFrame(decoded.width.toFloat(), decoded.height.toFloat())
                    }
                },
                compression = compression,
                onComplete = onComplete,
                onError = onError
            )
        }
    }

    fun onCaptureFailed(message: String) {
        autoCaptureTriggered = false
        _uiState.update {
            it.copy(
                isCapturing = false,
                statusMessage = message,
                phase = CameraCapturePhase.LIVE_PREVIEW
            )
        }
        refreshMotionState()
    }

    fun onExportFailed(message: String) {
        _uiState.update {
            it.copy(
                phase = if (it.capturedBitmap != null) {
                    CameraCapturePhase.POST_CAPTURE_ADJUST
                } else {
                    CameraCapturePhase.LIVE_PREVIEW
                },
                isCapturing = false,
                statusMessage = message
            )
        }
    }

    fun updateAdjustableCorner(index: Int, offset: Offset) {
        val corners = _uiState.value.adjustableCorners ?: return
        val point = PointF(offset.x, offset.y)
        val updated = when (index) {
            0 -> corners.copy(topLeft = point)
            1 -> corners.copy(topRight = point)
            2 -> corners.copy(bottomRight = point)
            else -> corners.copy(bottomLeft = point)
        }
        _uiState.update { it.copy(adjustableCorners = updated) }
    }

    fun snapCornersMagnetically() {
        val bitmap = _uiState.value.capturedBitmap ?: return
        val corners = _uiState.value.adjustableCorners ?: return
        _uiState.update {
            it.copy(
                adjustableCorners = DocumentCorners(
                    topLeft = DocumentScanProcessor.snapCornerToGradient(bitmap, corners.topLeft),
                    topRight = DocumentScanProcessor.snapCornerToGradient(bitmap, corners.topRight),
                    bottomRight = DocumentScanProcessor.snapCornerToGradient(bitmap, corners.bottomRight),
                    bottomLeft = DocumentScanProcessor.snapCornerToGradient(bitmap, corners.bottomLeft)
                )
            )
        }
    }

    fun confirmCapture(
        compression: ImageCompressionLevel,
        onComplete: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val bitmap = _uiState.value.capturedBitmap ?: return
        val corners = _uiState.value.adjustableCorners ?: return
        exportBitmap(bitmap, corners, compression, onComplete, onError)
    }

    fun resetToLivePreview() {
        _uiState.value.capturedBitmap?.recycle()
        previousCorners = null
        stableFrameCount = 0
        autoCaptureTriggered = false
        val flash = _uiState.value.flashMode
        val filter = _uiState.value.filterMode
        val auto = _uiState.value.autoCaptureEnabled
        _uiState.value = CameraCaptureUiState(
            flashMode = flash,
            filterMode = filter,
            autoCaptureEnabled = auto
        )
        refreshMotionState()
    }

    private fun exportBitmap(
        bitmap: Bitmap,
        corners: DocumentCorners,
        compression: ImageCompressionLevel,
        onComplete: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        _uiState.update { it.copy(phase = CameraCapturePhase.PROCESSING, isCapturing = true) }
        viewModelScope.launch {
            runCatching {
                DocumentScanProcessor.processAndExport(
                    context = getApplication(),
                    source = bitmap,
                    corners = corners,
                    filterMode = _uiState.value.filterMode,
                    compression = compression
                )
            }.onSuccess { uri ->
                bitmap.recycle()
                _uiState.update { CameraCaptureUiState(processedUri = uri) }
                onComplete(uri)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        phase = if (it.capturedBitmap != null) {
                            CameraCapturePhase.POST_CAPTURE_ADJUST
                        } else {
                            CameraCapturePhase.LIVE_PREVIEW
                        },
                        isCapturing = false,
                        statusMessage = error.localizedMessage.orEmpty()
                    )
                }
                onError(error.localizedMessage.orEmpty())
            }
        }
    }

    override fun onCleared() {
        _uiState.value.capturedBitmap?.recycle()
        stopMotionMonitoring()
        super.onCleared()
    }

    companion object {
        private const val STABLE_FRAME_THRESHOLD = 4
    }
}
