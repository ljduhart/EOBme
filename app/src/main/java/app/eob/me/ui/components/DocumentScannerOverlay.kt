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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.eob.me.data.DocumentBounds

private val OverlayMask = Color(0xB3000000)
private val LaserRed = Color(0xFFFF2D2D)
private val LaserRedBright = Color(0xFFFF6B6B)
private val EdgeDetectedGreen = Color(0xFF22C55E)
private val EdgeSearchingWhite = Color(0xFFE2E8F0)

@Composable
fun DocumentScannerOverlay(
    documentBounds: DocumentBounds?,
    isDocumentDetected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RedLaser")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserSweep"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val bounds = documentBounds?.takeIf { it.isDetected }
        val left: Float
        val top: Float
        val boxWidth: Float
        val boxHeight: Float

        if (bounds != null) {
            left = canvasWidth * bounds.left
            top = canvasHeight * bounds.top
            boxWidth = canvasWidth * (bounds.right - bounds.left)
            boxHeight = canvasHeight * (bounds.bottom - bounds.top)
        } else {
            boxWidth = canvasWidth * 0.82f
            boxHeight = minOf(boxWidth * 1.35f, canvasHeight * 0.72f)
            left = (canvasWidth - boxWidth) / 2f
            top = (canvasHeight - boxHeight) / 2f
        }

        val cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
        val guideSize = Size(boxWidth, boxHeight)
        val edgeColor = if (isDocumentDetected) EdgeDetectedGreen else EdgeSearchingWhite

        drawRect(color = OverlayMask, size = size)

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = guideSize,
            cornerRadius = cornerRadius,
            blendMode = BlendMode.Clear
        )

        val strokeWidth = 3.dp.toPx()
        drawRoundRect(
            color = edgeColor.copy(alpha = 0.95f),
            topLeft = Offset(left, top),
            size = guideSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        drawDocumentCornerBrackets(left, top, boxWidth, boxHeight, edgeColor, strokeWidth)

        val laserY = top + (boxHeight * sweepProgress)
        val laserHalfHeight = 18.dp.toPx()
        val laserGradient = Brush.verticalGradient(
            colors = listOf(
                LaserRed.copy(alpha = 0f),
                LaserRedBright.copy(alpha = 0.95f),
                LaserRed.copy(alpha = 0.95f),
                LaserRed.copy(alpha = 0f)
            ),
            startY = laserY - laserHalfHeight,
            endY = laserY + laserHalfHeight
        )

        drawRect(
            brush = laserGradient,
            topLeft = Offset(left + 6.dp.toPx(), laserY - laserHalfHeight),
            size = Size(boxWidth - 12.dp.toPx(), laserHalfHeight * 2f)
        )

        drawLine(
            color = LaserRedBright,
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(left + boxWidth - 8.dp.toPx(), laserY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDocumentCornerBrackets(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    color: Color,
    strokeWidth: Float
) {
    val cornerLen = minOf(width, height) * 0.12f
    val path = Path()
    path.moveTo(left, top + cornerLen)
    path.lineTo(left, top)
    path.lineTo(left + cornerLen, top)

    path.moveTo(left + width - cornerLen, top)
    path.lineTo(left + width, top)
    path.lineTo(left + width, top + cornerLen)

    path.moveTo(left + width, top + height - cornerLen)
    path.lineTo(left + width, top + height)
    path.lineTo(left + width - cornerLen, top + height)

    path.moveTo(left + cornerLen, top + height)
    path.lineTo(left, top + height)
    path.lineTo(left, top + height - cornerLen)

    drawPath(path = path, color = color, style = Stroke(width = strokeWidth * 1.4f))
}
