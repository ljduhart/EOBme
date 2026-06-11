package app.eob.me.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandCyan
import app.eob.me.ui.theme.EobCyberOverlay

private val OverlayMask = EobCyberOverlay
private val BrandBlue = EobBrandBlue
private val NeonTeal = EobBrandCyan

@Composable
fun CameraScanningOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserScanner")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserSweepProgress"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val boxWidth = canvasWidth * 0.85f
        var boxHeight = boxWidth * 1.414f
        if (boxHeight > canvasHeight * 0.85f) {
            boxHeight = canvasHeight * 0.85f
        }
        val left = (canvasWidth - boxWidth) / 2f
        val top = (canvasHeight - boxHeight) / 2f
        val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        val guideSize = Size(boxWidth, boxHeight)

        drawRect(color = OverlayMask, size = size)

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = guideSize,
            cornerRadius = cornerRadius,
            blendMode = BlendMode.Clear
        )

        drawRoundRect(
            color = BrandBlue.copy(alpha = 0.8f),
            topLeft = Offset(left, top),
            size = guideSize,
            cornerRadius = cornerRadius,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 20f), 0f)
            )
        )

        val laserY = top + (boxHeight * sweepProgress)
        val laserHalfHeight = 15.dp.toPx()
        val laserGradient = Brush.verticalGradient(
            colors = listOf(
                NeonTeal.copy(alpha = 0f),
                NeonTeal.copy(alpha = 0.9f),
                BrandBlue.copy(alpha = 0.9f),
                BrandBlue.copy(alpha = 0f)
            ),
            startY = laserY - laserHalfHeight,
            endY = laserY + laserHalfHeight
        )

        drawRect(
            brush = laserGradient,
            topLeft = Offset(left + 4.dp.toPx(), laserY - laserHalfHeight),
            size = Size(boxWidth - 8.dp.toPx(), laserHalfHeight * 2f)
        )
    }
}
