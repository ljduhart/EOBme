package app.eob.me.ui.components.bento

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.eob.me.navigation.HubBentoDestination

@Composable
fun HubBentoIcon(
    destination: HubBentoDestination,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(32.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.07f)
        when (destination) {
            HubBentoDestination.ProviderDirectory -> drawProviderIcon(tint, stroke)
            HubBentoDestination.EobHistory -> drawHistoryIcon(tint, stroke)
            HubBentoDestination.CptTracker -> drawCptIcon(tint, stroke)
            HubBentoDestination.YtdExpense -> drawExpenseIcon(tint, stroke)
            HubBentoDestination.InsuranceNews -> drawNewsIcon(tint, stroke)
            HubBentoDestination.AppealGenerator -> drawAppealIcon(tint, stroke)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProviderIcon(
    tint: Color,
    stroke: Stroke
) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.2f, h * 0.55f),
        size = Size(w * 0.6f, h * 0.3f),
        cornerRadius = CornerRadius(w * 0.06f)
    )
    drawPath(
        path = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.78f, h * 0.55f)
            lineTo(w * 0.22f, h * 0.55f)
            close()
        },
        color = tint
    )
    drawLine(tint, Offset(w * 0.42f, h * 0.7f), Offset(w * 0.42f, h * 0.82f), stroke.width)
    drawLine(tint, Offset(w * 0.58f, h * 0.7f), Offset(w * 0.58f, h * 0.82f), stroke.width)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHistoryIcon(
    tint: Color,
    stroke: Stroke
) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.22f, h * 0.18f),
        size = Size(w * 0.56f, h * 0.64f),
        cornerRadius = CornerRadius(w * 0.08f),
        style = stroke
    )
    repeat(3) { index ->
        val y = h * (0.32f + index * 0.16f)
        drawLine(tint, Offset(w * 0.32f, y), Offset(w * 0.68f, y), stroke.width * 1.1f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCptIcon(
    tint: Color,
    stroke: Stroke
) {
    val w = size.width
    val h = size.height
    drawCircle(tint, radius = w * 0.22f, center = Offset(w * 0.35f, h * 0.38f), style = stroke)
    drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.62f, h * 0.62f), style = stroke)
    drawLine(tint, Offset(w * 0.28f, h * 0.72f), Offset(w * 0.72f, h * 0.28f), stroke.width * 1.2f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawExpenseIcon(
    tint: Color,
    stroke: Stroke
) {
    val w = size.width
    val h = size.height
    val barWidths = listOf(0.14f, 0.14f, 0.14f, 0.14f)
    val heights = listOf(0.35f, 0.55f, 0.45f, 0.7f)
    var x = w * 0.18f
    heights.forEachIndexed { index, fraction ->
        val barW = w * barWidths[index]
        val barH = h * fraction
        drawRoundRect(
            color = tint,
            topLeft = Offset(x, h * 0.82f - barH),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(barW * 0.3f)
        )
        x += barW + w * 0.08f
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNewsIcon(
    tint: Color,
    stroke: Stroke
) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.2f, h * 0.2f),
        size = Size(w * 0.6f, h * 0.62f),
        cornerRadius = CornerRadius(w * 0.06f),
        style = stroke
    )
    drawLine(tint, Offset(w * 0.3f, h * 0.38f), Offset(w * 0.7f, h * 0.38f), stroke.width)
    drawLine(tint, Offset(w * 0.3f, h * 0.52f), Offset(w * 0.62f, h * 0.52f), stroke.width)
    drawLine(tint, Offset(w * 0.3f, h * 0.66f), Offset(w * 0.55f, h * 0.66f), stroke.width)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAppealIcon(
    tint: Color,
    stroke: Stroke
) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.24f, h * 0.16f),
        size = Size(w * 0.52f, h * 0.68f),
        cornerRadius = CornerRadius(w * 0.05f),
        style = stroke
    )
    drawPath(
        path = Path().apply {
            moveTo(w * 0.34f, h * 0.78f)
            lineTo(w * 0.5f, h * 0.9f)
            lineTo(w * 0.66f, h * 0.78f)
            close()
        },
        color = tint
    )
    drawLine(tint, Offset(w * 0.36f, h * 0.36f), Offset(w * 0.64f, h * 0.36f), stroke.width)
    drawLine(tint, Offset(w * 0.36f, h * 0.5f), Offset(w * 0.58f, h * 0.5f), stroke.width)
}
