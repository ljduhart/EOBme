package app.eob.me.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.eob.me.data.DocumentBounds
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight frame analyzer that estimates insurance-document edges for CameraX preview overlay.
 */
class DocumentEdgeAnalyzer(
    private val onBounds: (DocumentBounds?) -> Unit
) : ImageAnalysis.Analyzer {

    private val isProcessing = AtomicBoolean(false)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        try {
            val bounds = detectBounds(imageProxy)
            onBounds(bounds)
        } catch (_: Exception) {
            onBounds(null)
        } finally {
            isProcessing.set(false)
            imageProxy.close()
        }
    }

  private fun detectBounds(imageProxy: ImageProxy): DocumentBounds? {
        val image = imageProxy.image ?: return null
        val yPlane = image.planes.firstOrNull() ?: return null
        val buffer: ByteBuffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val width = image.width
        val height = image.height

        val sampleW = 120
        val sampleH = 160
        val stepX = max(1, width / sampleW)
        val stepY = max(1, height / sampleH)

        var minX = sampleW
        var maxX = 0
        var minY = sampleH
        var maxY = 0
        var edgeCount = 0

        fun luminanceAt(x: Int, y: Int): Int {
            val index = y * rowStride + x
            if (index < 0 || index >= buffer.capacity()) return 128
            return buffer.get(index).toInt() and 0xFF
        }

        for (sy in 1 until sampleH - 1) {
            val y = min(height - 2, sy * stepY)
            for (sx in 1 until sampleW - 1) {
                val x = min(width - 2, sx * stepX)
                val center = luminanceAt(x, y)
                val gx = abs(luminanceAt(x + stepX, y) - luminanceAt(x - stepX, y))
                val gy = abs(luminanceAt(x, y + stepY) - luminanceAt(x, y - stepY))
                val gradient = gx + gy
                if (gradient > 42) {
                    edgeCount++
                    if (sx < minX) minX = sx
                    if (sx > maxX) maxX = sx
                    if (sy < minY) minY = sy
                    if (sy > maxY) maxY = sy
                }
            }
        }

        if (edgeCount < 80 || maxX <= minX || maxY <= minY) return null

        val padX = (maxX - minX) * 0.04f
        val padY = (maxY - minY) * 0.04f
        val left = ((minX - padX) / sampleW).coerceIn(0.08f, 0.92f)
        val top = ((minY - padY) / sampleH).coerceIn(0.08f, 0.92f)
        val right = ((maxX + padX) / sampleW).coerceIn(0.08f, 0.92f)
        val bottom = ((maxY + padY) / sampleH).coerceIn(0.08f, 0.92f)

        return DocumentBounds(left, top, right, bottom)
    }
}
