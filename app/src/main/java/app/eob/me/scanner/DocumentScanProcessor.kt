package app.eob.me.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.net.Uri
import app.eob.me.data.ImageCompressionLevel
import app.eob.me.util.OcrProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

enum class ScanFilterMode {
    COLOR,
    GRAYSCALE,
    BLACK_WHITE
}

/**
 * Premium document post-processing: deskew, filters, cleaning, and export for the hybrid pipeline.
 */
object DocumentScanProcessor {
    suspend fun processAndExport(
        context: Context,
        source: Bitmap,
        corners: DocumentCorners,
        filterMode: ScanFilterMode,
        compression: ImageCompressionLevel = ImageCompressionLevel.Medium
    ): Uri = withContext(Dispatchers.IO) {
        var working = perspectiveWarp(source, corners)
        if (working !== source) source.recycle()
        working = correctOrientation(context, working)
        working = removeShadows(working)
        working = reduceMoire(working)
        working = cleanDocumentArtifacts(working)
        working = applyFilter(working, filterMode)
        val uri = exportCompressedJpeg(context, working, compression)
        working.recycle()
        uri
    }

    fun perspectiveWarp(bitmap: Bitmap, corners: DocumentCorners): Bitmap {
        val outputWidth = max(
            distance(corners.topLeft, corners.topRight),
            distance(corners.bottomLeft, corners.bottomRight)
        ).toInt().coerceIn(320, 4096)
        val outputHeight = max(
            distance(corners.topLeft, corners.bottomLeft),
            distance(corners.topRight, corners.bottomRight)
        ).toInt().coerceIn(320, 4096)

        val src = floatArrayOf(
            corners.topLeft.x, corners.topLeft.y,
            corners.topRight.x, corners.topRight.y,
            corners.bottomRight.x, corners.bottomRight.y,
            corners.bottomLeft.x, corners.bottomLeft.y
        )
        val dst = floatArrayOf(
            0f, 0f,
            outputWidth.toFloat(), 0f,
            outputWidth.toFloat(), outputHeight.toFloat(),
            0f, outputHeight.toFloat()
        )
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    suspend fun correctOrientation(context: Context, bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
        val text = runCatching { OcrProcessor.recognizeFromBitmap(bitmap) }.getOrDefault("")
        if (text.isBlank()) return@withContext bitmap
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return@withContext bitmap
        val avgLineLength = lines.sumOf { it.length } / lines.size
        if (avgLineLength > 12) return@withContext bitmap
        val rotated = Bitmap.createBitmap(bitmap.height, bitmap.width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rotated)
        val matrix = Matrix().apply { postRotate(90f) }
        canvas.drawBitmap(bitmap, matrix, null)
        if (rotated !== bitmap) bitmap.recycle()
        rotated
    }

    fun removeShadows(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val matrix = ColorMatrix(
            floatArrayOf(
                1.15f, 0f, 0f, 0f, 18f,
                0f, 1.15f, 0f, 0f, 18f,
                0f, 0f, 1.15f, 0f, 18f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        if (output !== bitmap) bitmap.recycle()
        return output
    }

    fun reduceMoire(bitmap: Bitmap): Bitmap {
        val scaledDown = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * 0.5f).toInt().coerceAtLeast(1),
            (bitmap.height * 0.5f).toInt().coerceAtLeast(1),
            true
        )
        val restored = Bitmap.createScaledBitmap(scaledDown, bitmap.width, bitmap.height, true)
        scaledDown.recycle()
        if (restored !== bitmap) bitmap.recycle()
        return restored
    }

    fun cleanDocumentArtifacts(bitmap: Bitmap): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(output.width * output.height)
        output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val luminance = colorLuminance(pixel)
            if (luminance > 245) {
                pixels[index] = Color.WHITE
            } else if (luminance < 28) {
                pixels[index] = Color.BLACK
            }
        }
        output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
        if (output !== bitmap) bitmap.recycle()
        return output
    }

    fun applyFilter(bitmap: Bitmap, mode: ScanFilterMode): Bitmap {
        return when (mode) {
            ScanFilterMode.COLOR -> bitmap
            ScanFilterMode.GRAYSCALE -> toGrayscale(bitmap)
            ScanFilterMode.BLACK_WHITE -> toHighContrastBinary(bitmap)
        }
    }

    fun snapCornerToGradient(bitmap: Bitmap, corner: PointF, radius: Int = 36): PointF {
        val left = (corner.x - radius).toInt().coerceIn(0, bitmap.width - 1)
        val top = (corner.y - radius).toInt().coerceIn(0, bitmap.height - 1)
        val right = (corner.x + radius).toInt().coerceIn(0, bitmap.width - 1)
        val bottom = (corner.y + radius).toInt().coerceIn(0, bitmap.height - 1)
        var best = corner
        var bestScore = Float.NEGATIVE_INFINITY
        for (y in top..bottom step 4) {
            for (x in left..right step 4) {
                val score = localContrast(bitmap, x, y)
                if (score > bestScore) {
                    bestScore = score
                    best = PointF(x.toFloat(), y.toFloat())
                }
            }
        }
        return best
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        if (output !== bitmap) bitmap.recycle()
        return output
    }

    private fun toHighContrastBinary(bitmap: Bitmap): Bitmap {
        val gray = toGrayscale(bitmap)
        val output = Bitmap.createBitmap(gray.width, gray.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(gray.width * gray.height)
        gray.getPixels(pixels, 0, gray.width, 0, 0, gray.width, gray.height)
        for (index in pixels.indices) {
            val luminance = colorLuminance(pixels[index])
            pixels[index] = if (luminance >= 170) Color.WHITE else Color.BLACK
        }
        output.setPixels(pixels, 0, gray.width, 0, 0, gray.width, gray.height)
        gray.recycle()
        if (output !== bitmap) bitmap.recycle()
        return output
    }

    private fun exportCompressedJpeg(
        context: Context,
        bitmap: Bitmap,
        compression: ImageCompressionLevel
    ): Uri {
        val file = File(context.cacheDir, "eob_camera_processed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, compression.jpegQuality, output)
        }
        return Uri.fromFile(file)
    }

    private fun distance(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun colorLuminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299f * r + 0.587f * g + 0.114f * b).toInt()
    }

    private fun localContrast(bitmap: Bitmap, x: Int, y: Int): Float {
        val center = colorLuminance(bitmap.getPixel(x, y))
        val left = colorLuminance(bitmap.getPixel(max(x - 2, 0), y))
        val right = colorLuminance(bitmap.getPixel(min(x + 2, bitmap.width - 1), y))
        val up = colorLuminance(bitmap.getPixel(x, max(y - 2, 0)))
        val down = colorLuminance(bitmap.getPixel(x, min(y + 2, bitmap.height - 1)))
        return abs(left - center).toFloat() +
            abs(right - center).toFloat() +
            abs(up - center).toFloat() +
            abs(down - center).toFloat()
    }

    private fun abs(value: Int): Int = if (value < 0) -value else value
}
