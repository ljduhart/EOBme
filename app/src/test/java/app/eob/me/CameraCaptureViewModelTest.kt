package app.eob.me

import android.app.Application
import app.eob.me.scanner.DocumentCorners
import app.eob.me.scanner.DocumentEdgeDetector
import app.eob.me.scanner.ScanFilterMode
import app.eob.me.viewmodel.CameraCaptureViewModel
import app.eob.me.viewmodel.CameraFlashMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CameraCaptureViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: CameraCaptureViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        application = RuntimeEnvironment.getApplication()
        viewModel = CameraCaptureViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cycleFlashMode_rotatesThroughModes() {
        assertEquals(CameraFlashMode.AUTO, viewModel.uiState.value.flashMode)
        viewModel.cycleFlashMode()
        assertEquals(CameraFlashMode.ON, viewModel.uiState.value.flashMode)
        viewModel.cycleFlashMode()
        assertEquals(CameraFlashMode.OFF, viewModel.uiState.value.flashMode)
    }

    @Test
    fun cycleFilterMode_rotatesThroughModes() {
        assertEquals(ScanFilterMode.COLOR, viewModel.uiState.value.filterMode)
        viewModel.cycleFilterMode()
        assertEquals(ScanFilterMode.GRAYSCALE, viewModel.uiState.value.filterMode)
        viewModel.cycleFilterMode()
        assertEquals(ScanFilterMode.BLACK_WHITE, viewModel.uiState.value.filterMode)
    }

    @Test
    fun toggleAutoCapture_flipsEnabledFlag() {
        assertTrue(viewModel.uiState.value.autoCaptureEnabled)
        viewModel.toggleAutoCapture()
        assertFalse(viewModel.uiState.value.autoCaptureEnabled)
    }

    @Test
    fun stableCornerFramesMeetAutoCaptureThreshold() {
        val corners = DocumentCorners.guideFrame(480f, 640f)
        var previous: DocumentCorners? = null
        var stableCount = 0
        repeat(5) {
            val stable = DocumentEdgeDetector.isStable(previous, corners)
            stableCount = if (stable) stableCount + 1 else 0
            previous = corners
        }
        assertTrue(stableCount >= 4)
        assertTrue(viewModel.uiState.value.autoCaptureEnabled)
    }

    @Test
    fun markAutoCaptureTriggered_preventsDuplicateCapture() {
        viewModel.markAutoCaptureTriggered()
        assertFalse(viewModel.shouldTriggerAutoCapture())
    }
}
