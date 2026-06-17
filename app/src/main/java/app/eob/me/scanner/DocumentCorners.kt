package app.eob.me.scanner

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset

data class DocumentCorners(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
) {
    fun toPolygonOffsets(): List<Offset> = listOf(
        Offset(topLeft.x, topLeft.y),
        Offset(topRight.x, topRight.y),
        Offset(bottomRight.x, bottomRight.y),
        Offset(bottomLeft.x, bottomLeft.y)
    )

    fun mapToView(
        sourceWidth: Float,
        sourceHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        scaleMode: ImageScaleMode = ImageScaleMode.FIT
    ): DocumentCorners {
        val layout = FittedImageLayout.forBitmap(sourceWidth, sourceHeight, viewWidth, viewHeight, scaleMode)
        return mapWith { point -> layout.bitmapToView(point) }
    }

    fun mapFromView(
        sourceWidth: Float,
        sourceHeight: Float,
        viewWidth: Float,
        viewHeight: Float,
        scaleMode: ImageScaleMode = ImageScaleMode.FIT
    ): (Offset) -> PointF {
        val layout = FittedImageLayout.forBitmap(sourceWidth, sourceHeight, viewWidth, viewHeight, scaleMode)
        return { offset -> layout.viewToBitmap(offset) }
    }

    fun scaleTo(
        fromWidth: Float,
        fromHeight: Float,
        toWidth: Float,
        toHeight: Float
    ): DocumentCorners {
        if (fromWidth <= 0f || fromHeight <= 0f) {
            return guideFrame(toWidth, toHeight)
        }
        val scaleX = toWidth / fromWidth
        val scaleY = toHeight / fromHeight
        return DocumentCorners(
            topLeft = PointF(topLeft.x * scaleX, topLeft.y * scaleY),
            topRight = PointF(topRight.x * scaleX, topRight.y * scaleY),
            bottomRight = PointF(bottomRight.x * scaleX, bottomRight.y * scaleY),
            bottomLeft = PointF(bottomLeft.x * scaleX, bottomLeft.y * scaleY)
        )
    }

    private fun mapWith(transform: (PointF) -> PointF): DocumentCorners {
        return DocumentCorners(
            topLeft = transform(topLeft),
            topRight = transform(topRight),
            bottomRight = transform(bottomRight),
            bottomLeft = transform(bottomLeft)
        )
    }

    companion object {
        fun guideFrame(width: Float, height: Float, insetRatio: Float = 0.08f): DocumentCorners {
            val insetX = width * insetRatio
            val insetY = height * insetRatio
            return DocumentCorners(
                topLeft = PointF(insetX, insetY),
                topRight = PointF(width - insetX, insetY),
                bottomRight = PointF(width - insetX, height - insetY),
                bottomLeft = PointF(insetX, height - insetY)
            )
        }
    }
}

/**
 * Maps bitmap pixel coordinates to a preview region inside a view.
 */
enum class ImageScaleMode {
    /** Matches PreviewView.ScaleType.FIT_CENTER and Compose ContentScale.Fit. */
    FIT,
    /** Matches PreviewView.ScaleType.FILL_CENTER. */
    FILL
}

data class FittedImageLayout(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val bitmapWidth: Float,
    val bitmapHeight: Float
) {
    fun bitmapToView(point: PointF): PointF {
        return PointF(
            offsetX + point.x * scale,
            offsetY + point.y * scale
        )
    }

    fun viewToBitmap(offset: Offset): PointF {
        val x = ((offset.x - offsetX) / scale).coerceIn(0f, bitmapWidth)
        val y = ((offset.y - offsetY) / scale).coerceIn(0f, bitmapHeight)
        return PointF(x, y)
    }

    companion object {
        fun forBitmap(
            bitmapWidth: Float,
            bitmapHeight: Float,
            viewWidth: Float,
            viewHeight: Float,
            scaleMode: ImageScaleMode = ImageScaleMode.FIT
        ): FittedImageLayout {
            val scale = when (scaleMode) {
                ImageScaleMode.FIT -> minOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
                ImageScaleMode.FILL -> maxOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
            }
            val displayWidth = bitmapWidth * scale
            val displayHeight = bitmapHeight * scale
            return FittedImageLayout(
                scale = scale,
                offsetX = (viewWidth - displayWidth) / 2f,
                offsetY = (viewHeight - displayHeight) / 2f,
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight
            )
        }
    }
}
