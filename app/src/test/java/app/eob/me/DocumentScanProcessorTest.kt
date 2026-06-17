package app.eob.me

import android.graphics.Bitmap
import android.graphics.Color
import app.eob.me.scanner.DocumentCorners
import app.eob.me.scanner.DocumentScanProcessor
import app.eob.me.scanner.ScanFilterMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DocumentScanProcessorTest {
    @Test
    fun perspectiveWarp_producesExpectedDimensions() {
        val source = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        val corners = DocumentCorners.guideFrame(800f, 1200f, insetRatio = 0.1f)
        val warped = DocumentScanProcessor.perspectiveWarp(source, corners)
        assertTrue(warped.width in 320..4096)
        assertTrue(warped.height in 320..4096)
        source.recycle()
        warped.recycle()
    }

    @Test
    fun applyFilter_blackWhiteProducesBinaryPixels() {
        val source = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.LTGRAY)
        val filtered = DocumentScanProcessor.applyFilter(source, ScanFilterMode.BLACK_WHITE)
        val pixel = filtered.getPixel(10, 10)
        val rgb = listOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
        assertTrue(rgb.all { it == 0 || it == 255 })
        if (filtered !== source) source.recycle()
        filtered.recycle()
    }
}
