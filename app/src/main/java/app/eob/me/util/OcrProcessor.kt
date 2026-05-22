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
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object OcrProcessor {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeFromBitmap(bitmap: Bitmap): String {
        return recognizeImage(InputImage.fromBitmap(bitmap, 0))
    }

    suspend fun recognizeFromUri(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        return if (mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
            recognizePdf(context, uri)
        } else {
            recognizeImage(InputImage.fromFilePath(context, uri))
        }
    }

    fun prepareUriForUpload(context: Context, uri: Uri): Uri {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        if (mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) return uri

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("Unable to decode selected EOB image")
        return prepareBitmapForUpload(context, decoded)
    }

    fun prepareBitmapForUpload(context: Context, bitmap: Bitmap): Uri {
        val scaled = scaleBitmap(bitmap)
        val enhanced = enhanceContrast(scaled)
        val file = File(context.cacheDir, "eob_upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output ->
            enhanced.compress(Bitmap.CompressFormat.JPEG, 88, output)
        }
        if (scaled !== bitmap) scaled.recycle()
        if (enhanced !== scaled) enhanced.recycle()
        return Uri.fromFile(file)
    }

    private suspend fun recognizePdf(context: Context, uri: Uri): String {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return ""
        return descriptor.use { fileDescriptor ->
            PdfRenderer(fileDescriptor).use { renderer ->
                buildString {
                    repeat(renderer.pageCount) { pageIndex ->
                        renderer.openPage(pageIndex).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                page.width.coerceAtLeast(1) * 2,
                                page.height.coerceAtLeast(1) * 2,
                                Bitmap.Config.ARGB_8888
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

    private suspend fun recognizeImage(image: InputImage): String {
        return suspendCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text -> continuation.resume(text.text) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
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
        return sampleSize.coerceAtLeast(1)
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