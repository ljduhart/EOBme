package app.eob.me.scanner

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight on-device document edge detection for CameraX preview frames.
 * Uses gradient scoring on a downscaled bitmap — no OpenCV dependency.
 */
object DocumentEdgeDetector {
    private const val ANALYSIS_MAX_WIDTH = 360
    private const val STABILITY_THRESHOLD_PX = 18f

    fun detectCorners(bitmap: Bitmap): DocumentCorners {
        val working = downscale(bitmap, ANALYSIS_MAX_WIDTH)
        val width = working.width
        val height = working.height
        val grayscale = toGrayscale(working)
        if (working !== bitmap) working.recycle()

        val left = findVerticalEdge(grayscale, width, height, fromLeft = true)
        val right = findVerticalEdge(grayscale, width, height, fromLeft = false)
        val top = findHorizontalEdge(grayscale, width, height, fromTop = true)
        val bottom = findHorizontalEdge(grayscale, width, height, fromTop = false)

        val scaleX = bitmap.width.toFloat() / width
        val scaleY = bitmap.height.toFloat() / height
        return DocumentCorners(
            topLeft = PointF(left * scaleX, top * scaleY),
            topRight = PointF(right * scaleX, top * scaleY),
            bottomRight = PointF(right * scaleX, bottom * scaleY),
            bottomLeft = PointF(left * scaleX, bottom * scaleY)
        )
    }

    fun isStable(previous: DocumentCorners?, current: DocumentCorners): Boolean {
        if (previous == null) return false
        return cornerDistance(previous.topLeft, current.topLeft) < STABILITY_THRESHOLD_PX &&
            cornerDistance(previous.topRight, current.topRight) < STABILITY_THRESHOLD_PX &&
            cornerDistance(previous.bottomRight, current.bottomRight) < STABILITY_THRESHOLD_PX &&
            cornerDistance(previous.bottomLeft, current.bottomLeft) < STABILITY_THRESHOLD_PX
    }

    private fun cornerDistance(a: PointF, b: PointF): Float = hypot(a.x - b.x, a.y - b.y)

    private fun downscale(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val scale = maxWidth.toFloat() / bitmap.width.toFloat()
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, maxWidth, targetHeight, true)
    }

    private fun toGrayscale(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.map { pixel ->
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }.toIntArray()
    }

    private fun findVerticalEdge(
        grayscale: IntArray,
        width: Int,
        height: Int,
        fromLeft: Boolean
    ): Int {
        val xRange = if (fromLeft) 0 until width / 2 else width - 1 downTo width / 2
        var bestX = if (fromLeft) (width * 0.08f).toInt() else (width * 0.92f).toInt()
        var bestScore = Float.NEGATIVE_INFINITY
        for (x in xRange) {
            var score = 0f
            for (y in 1 until height - 1) {
                val left = grayscale[y * width + max(x - 1, 0)]
                val right = grayscale[y * width + min(x + 1, width - 1)]
                score += abs(right - left)
            }
            if (score > bestScore) {
                bestScore = score
                bestX = x
            }
        }
        return bestX
    }

    private fun findHorizontalEdge(
        grayscale: IntArray,
        width: Int,
        height: Int,
        fromTop: Boolean
    ): Int {
        val yRange = if (fromTop) 0 until height / 2 else height - 1 downTo height / 2
        var bestY = if (fromTop) (height * 0.08f).toInt() else (height * 0.92f).toInt()
        var bestScore = Float.NEGATIVE_INFINITY
        for (y in yRange) {
            var score = 0f
            for (x in 1 until width - 1) {
                val up = grayscale[max(y - 1, 0) * width + x]
                val down = grayscale[min(y + 1, height - 1) * width + x]
                score += abs(down - up)
            }
            if (score > bestScore) {
                bestScore = score
                bestY = y
            }
        }
        return bestY
    }
}
