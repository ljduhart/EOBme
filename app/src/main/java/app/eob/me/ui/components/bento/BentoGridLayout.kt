package app.eob.me.ui.components.bento

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive spacing and sizing for the hub bento grid (3-column rows on HomeScreen).
 */
object BentoGridLayout {
    const val GRID_COLUMNS = 3
    private val MIN_CELL_WIDTH = 96.dp
    private val MAX_CELL_WIDTH = 220.dp

    /** Horizontal gap between bento cells — scales with row width, clamped for legibility. */
    fun spacing(rowWidth: Dp): Dp = (rowWidth * 0.065f).coerceIn(14.dp, 28.dp)

    /**
     * Width-aware aspect ratio (width / height). Narrower cells stay taller;
     * wider cells scale proportionally toward [BentoCellLayout.LEGACY_ASPECT_RATIO].
     */
    fun aspectRatioForCellWidth(cellWidth: Dp): Float {
        if (cellWidth <= 0.dp) return BentoCellLayout.ASPECT_RATIO
        val fraction = ((cellWidth - MIN_CELL_WIDTH) / (MAX_CELL_WIDTH - MIN_CELL_WIDTH))
            .coerceIn(0f, 1f)
        val delta = BentoCellLayout.LEGACY_ASPECT_RATIO - BentoCellLayout.ASPECT_RATIO
        return BentoCellLayout.ASPECT_RATIO + fraction * delta * 0.45f
    }
}

@Composable
fun BentoCellTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    maxLines: Int = 1
) {
    Text(
        text = text,
        style = style,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
