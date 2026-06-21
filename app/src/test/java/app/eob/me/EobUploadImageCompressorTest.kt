package app.eob.me

import android.graphics.Bitmap
import android.graphics.Color
import app.eob.me.util.EobUploadImageCompressor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EobUploadImageCompressorTest {
    @Test
    fun calculateInSampleSizeDownscalesLargeCameraFrames() {
        val sampleSize = EobUploadImageCompressor.calculateInSampleSize(4032, 3024, 1920)
        assertTrue(sampleSize >= 2)
    }

    @Test
    fun scaleBitmapCapsLongestEdgeAt1920() {
        val bitmap = Bitmap.createBitmap(3000, 4000, Bitmap.Config.ARGB_8888)
        val scaled = EobUploadImageCompressor.scaleBitmap(bitmap, EobUploadImageCompressor.MAX_DIMENSION)
        assertEquals(1440, scaled.width)
        assertEquals(1920, scaled.height)
        bitmap.recycle()
        scaled.recycle()
    }

    @Test
    fun compressToTargetSizeUsesSixtyFivePercentQualityAndStaysUnderTarget() {
        val bitmap = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        val bytes = EobUploadImageCompressor.compressToTargetSize(bitmap)
        bitmap.recycle()
        assertTrue(bytes.isNotEmpty())
        assertTrue(bytes.size <= EobUploadImageCompressor.TARGET_MAX_BYTES)
    }

    @Test
    fun uploadCompressionConstantsMatchVeryfiUploadGuidance() {
        assertEquals(1920, EobUploadImageCompressor.MAX_DIMENSION)
        assertEquals(65, EobUploadImageCompressor.INITIAL_JPEG_QUALITY)
        assertTrue(EobUploadImageCompressor.INITIAL_JPEG_QUALITY in 60..70)
        assertEquals(500_000, EobUploadImageCompressor.TARGET_MAX_BYTES)
    }
}
