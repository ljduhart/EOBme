package app.eob.me.data

/**
 * Normalized document rectangle in preview coordinates (0f–1f).
 */
data class DocumentBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val isDetected: Boolean
        get() {
            val width = right - left
            val height = bottom - top
            return width in 0.2f..0.95f && height in 0.15f..0.95f && width < height * 1.6f
        }
}
