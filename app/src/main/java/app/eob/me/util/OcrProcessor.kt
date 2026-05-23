package app.eob.me.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OcrProcessor {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeFromBitmap(bitmap: Bitmap): String {
        return recognizeImage(InputImage.fromBitmap(bitmap, 0))
    }

    suspend fun recognizeFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        if (mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
            recognizePdf(context, uri)
        } else {
            recognizeImage(InputImage.fromFilePath(context, uri))
        }
    }

    // Shifted to Dispatchers.IO to prevent UI Stalling
    suspend fun prepareUriForUpload(context: Context, uri: Uri): Uri = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        if (mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) return@withContext uri

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            // Config.RGB_565 halves memory consumption and prevents OOM on large camera uploads
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("Unable to decode selected EOB image")

        return@withContext prepareBitmapForUpload(context, decoded)
    }

    suspend fun prepareBitmapForUpload(context: Context, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val scaled = scaleBitmap(bitmap)
        val enhanced = enhanceContrast(scaled)

        val file = File(context.cacheDir, "eob_upload_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(file).use { output ->
                // Drop quality slightly to 85% for vastly reduced payload file size without losing OCR accuracy
                enhanced.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }
            Uri.fromFile(file)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
            if (enhanced !== scaled) enhanced.recycle()
            bitmap.recycle()
        }
    }

    private suspend fun recognizePdf(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext ""
        return@withContext descriptor.use { fileDescriptor ->
            PdfRenderer(fileDescriptor).use { renderer ->
                buildString {
                    repeat(renderer.pageCount) { pageIndex ->
                        renderer.openPage(pageIndex).use { page ->
                            // Uses RGB_565 here too to ensure multi-page PDFs don't swamp device RAM
                            val bitmap = Bitmap.createBitmap(
                                page.width.coerceAtLeast(1) * 2,
                                page.height.coerceAtLeast(1) * 2,
                                Bitmap.Config.RGB_565
                            )
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            appendLine(recognizeFromBitmap(bitmap))
                            bitmap.recycle()
                        }
                    }
                }
            }
        }
    }

    // Upgraded to suspendCancellableCoroutine to handle execution scope interruptions safely
    private suspend fun recognizeImage(image: InputImage): String {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    if (continuation.isActive) continuation.resume(text.text)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }

            continuation.invokeOnCancellation {
                // If the routine is cancelled, clean up memory states
            }
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int = 1800): Int {
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

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int = 1800): Bitmap {
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

    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val contrast = 1.25f
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