package app.eob.me.util

import android.graphics.Bitmap

/**
 * Crops a captured scan to the same guide proportions used by the camera capture edge overlay.
 */
object DocumentScanCrop {
    fun applyGuideCrop(bitmap: Bitmap): Bitmap {
        val canvasWidth = bitmap.width.toFloat()
        val canvasHeight = bitmap.height.toFloat()
        var boxWidth = canvasWidth * 0.85f
        var boxHeight = boxWidth * 1.414f
        if (boxHeight > canvasHeight * 0.85f) {
            boxHeight = canvasHeight * 0.85f
            boxWidth = boxHeight / 1.414f
        }
        val left = ((canvasWidth - boxWidth) / 2f).toInt().coerceAtLeast(0)
        val top = ((canvasHeight - boxHeight) / 2f).toInt().coerceAtLeast(0)
        val width = boxWidth.toInt().coerceIn(1, bitmap.width - left)
        val height = boxHeight.toInt().coerceIn(1, bitmap.height - top)
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}
