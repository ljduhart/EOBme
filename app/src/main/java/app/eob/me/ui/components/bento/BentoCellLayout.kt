package app.eob.me.ui.components.bento

/**
 * Shared bento grid cell dimensions — all hub tiles use the same aspect ratio.
 *
 * Width-to-height ratio. Lower values yield taller cells. [LEGACY_ASPECT_RATIO]
 * was 1.35f; [ASPECT_RATIO] is 50% taller for mobile legibility (1.35 / 1.5 ≈ 0.9).
 */
object BentoCellLayout {
    const val LEGACY_ASPECT_RATIO = 1.35f
    const val ASPECT_RATIO = LEGACY_ASPECT_RATIO / 1.5f
}
