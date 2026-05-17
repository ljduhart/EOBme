package app.eob.me.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
}
