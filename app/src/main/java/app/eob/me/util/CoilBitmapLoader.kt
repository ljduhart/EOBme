package app.eob.me.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File

/**
 * Downsampling bitmap loader backed by Coil so large camera captures and remote
 * evidence images are decoded with bounded memory instead of full-resolution
 * [android.graphics.BitmapFactory] decodes.
 */
object CoilBitmapLoader {
    const val DEFAULT_MAX_DIMENSION = 1024

    suspend fun loadBitmap(
        context: Context,
        data: Any,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Bitmap? {
        if (maxDimension <= 0) return null
        val request = ImageRequest.Builder(context)
            .data(data)
            .size(maxDimension)
            .allowHardware(false)
            .build()
        return when (val result = context.imageLoader.execute(request)) {
            is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
            else -> null
        }
    }

    suspend fun loadBitmapFromFile(
        context: Context,
        filePath: String,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Bitmap? {
        val file = File(filePath)
        if (!file.isFile) return null
        return loadBitmap(context, file, maxDimension)
    }

    suspend fun loadBitmapFromUrl(
        context: Context,
        url: String,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Bitmap? {
        if (url.isBlank()) return null
        return loadBitmap(context, url, maxDimension)
    }
}
