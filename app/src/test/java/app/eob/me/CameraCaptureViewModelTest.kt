package app.eob.me

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import app.eob.me.scanner.ScanFilterMode
import app.eob.me.viewmodel.CameraCapturePhase
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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
    fun shouldTriggerAutoCapture_whenStableAndEnabled() {
        viewModel.toggleAutoCapture()
        viewModel.toggleAutoCapture()
        repeat(5) {
            val bitmap = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            viewModel.onPreviewFrame(bitmap)
        }
        viewModel.refreshMotionState()
        assertTrue(viewModel.uiState.value.autoCaptureActive)
        assertTrue(viewModel.shouldTriggerAutoCapture())
    }

    @Test
    fun markAutoCaptureTriggered_preventsDuplicateCapture() {
        viewModel.markAutoCaptureTriggered()
        assertFalse(viewModel.shouldTriggerAutoCapture())
    }
}
