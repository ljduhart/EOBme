package app.eob.me

import android.graphics.Bitmap
import android.graphics.Color
import app.eob.me.scanner.DocumentCorners
import app.eob.me.scanner.DocumentEdgeDetector
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentEdgeDetectorTest {
    @Test
    fun detectCorners_returnsQuadrilateralInsideFrame() {
        val bitmap = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(Color.DKGRAY)
        canvas.drawRect(60f, 80f, 420f, 560f, android.graphics.Paint().apply { color = Color.WHITE })

        val corners = DocumentEdgeDetector.detectCorners(bitmap)
        assertNotNull(corners)
        assertTrue(corners.topLeft.x < corners.topRight.x)
        assertTrue(corners.topLeft.y < corners.bottomLeft.y)
        bitmap.recycle()
    }

    @Test
    fun isStable_requiresSimilarCorners() {
        val stable = DocumentCorners.guideFrame(1000f, 1400f)
        val shifted = stable.copy(
            topLeft = android.graphics.PointF(stable.topLeft.x + 2f, stable.topLeft.y + 2f)
        )
        assertTrue(DocumentEdgeDetector.isStable(stable, shifted))
    }
}
