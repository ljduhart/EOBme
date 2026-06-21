package app.eob.me.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Upload-optimized JPEG compression for the hybrid Veryfi/Firebase pipeline.
 * Veryfi OCR does not need full camera resolution; smaller payloads upload and extract faster.
 */
object EobUploadImageCompressor {
    const val MAX_DIMENSION = 1920
    const val INITIAL_JPEG_QUALITY = 65
    const val MIN_JPEG_QUALITY = 55
    const val TARGET_MAX_BYTES = 500_000

    suspend fun compressUriForUpload(context: Context, uri: Uri): Uri = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        if (mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
            return@withContext uri
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalArgumentException("Unable to decode EOB image for upload compression.")

        compressBitmapToUploadUri(context, decoded)
    }

    fun compressBitmapToUploadUri(context: Context, bitmap: Bitmap): Uri {
        val scaled = scaleBitmap(bitmap, MAX_DIMENSION)
        val enhanced = enhanceContrastForOcr(scaled)
        val jpegBytes = compressToTargetSize(enhanced)
        if (scaled !== bitmap) scaled.recycle()
        if (enhanced !== scaled) enhanced.recycle()
        bitmap.recycle()

        val file = File(context.cacheDir, "eob_upload_compressed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(jpegBytes) }
        return Uri.fromFile(file)
    }

    internal fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var candidateWidth = width
        var candidateHeight = height
        while (candidateWidth / 2 >= maxDimension || candidateHeight / 2 >= maxDimension) {
            candidateWidth /= 2
            candidateHeight /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    internal fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    internal fun compressToTargetSize(bitmap: Bitmap): ByteArray {
        var quality = INITIAL_JPEG_QUALITY
        var bytes = encodeJpeg(bitmap, quality)
        while (bytes.size > TARGET_MAX_BYTES && quality > MIN_JPEG_QUALITY) {
            quality -= 5
            bytes = encodeJpeg(bitmap, quality)
        }
        return bytes
    }

    private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }

    private fun enhanceContrastForOcr(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val contrast = 1.2f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val matrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }
}
