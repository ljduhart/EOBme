package app.eob.me.ui.components

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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

private val PillBottleAmber = Color(0xFFF9A825)
private val PillBottleAmberDark = Color(0xFFE68A00)
private val PillBottleCap = Color(0xFFF5F5F5)
private val PillBottleLabel = Color(0xFFFFFFFF)
private val PillCapsuleRed = Color(0xFFE53935)
private val PillCapsuleBlue = Color(0xFF1E88E5)
private val NotepadPaper = Color(0xFFFFF8E1)
private val NotepadLine = Color(0xFFB0BEC5)
private val NotepadSpiral = Color(0xFF78909C)

@Composable
fun InsuranceCardPillBottleIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(52.dp)) {
        val width = size.width
        val height = size.height
        val bodyLeft = width * 0.22f
        val bodyRight = width * 0.78f
        val bodyTop = height * 0.28f
        val bodyBottom = height * 0.92f
        val capTop = height * 0.06f
        val capBottom = bodyTop + height * 0.02f

        drawRoundRect(
            color = PillBottleCap,
            topLeft = Offset(bodyLeft - width * 0.04f, capTop),
            size = Size(bodyRight - bodyLeft + width * 0.08f, capBottom - capTop),
            cornerRadius = CornerRadius(width * 0.04f, width * 0.04f)
        )
        repeat(5) { index ->
            val x = bodyLeft - width * 0.02f + index * ((bodyRight - bodyLeft + width * 0.04f) / 4f)
            drawLine(
                color = Color(0x33000000),
                start = Offset(x, capTop + height * 0.02f),
                end = Offset(x, capBottom - height * 0.01f),
                strokeWidth = width * 0.012f
            )
        }

        drawRoundRect(
            color = PillBottleAmber,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(width * 0.08f, width * 0.08f)
        )
        drawRoundRect(
            color = PillBottleAmberDark.copy(alpha = 0.22f),
            topLeft = Offset(bodyLeft + width * 0.04f, bodyTop),
            size = Size(width * 0.08f, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(width * 0.04f, width * 0.04f)
        )

        val labelTop = bodyTop + height * 0.12f
        val labelHeight = height * 0.34f
        drawRoundRect(
            color = PillBottleLabel,
            topLeft = Offset(bodyLeft + width * 0.08f, labelTop),
            size = Size(bodyRight - bodyLeft - width * 0.16f, labelHeight),
            cornerRadius = CornerRadius(width * 0.03f, width * 0.03f)
        )

        val capsuleCenter = Offset(width / 2f, labelTop + labelHeight / 2f)
        rotate(degrees = -35f, pivot = capsuleCenter) {
            drawRoundRect(
                color = PillCapsuleRed,
                topLeft = Offset(capsuleCenter.x - width * 0.11f, capsuleCenter.y - height * 0.04f),
                size = Size(width * 0.11f, height * 0.08f),
                cornerRadius = CornerRadius(height * 0.04f, height * 0.04f)
            )
            drawRoundRect(
                color = PillCapsuleBlue,
                topLeft = Offset(capsuleCenter.x, capsuleCenter.y - height * 0.04f),
                size = Size(width * 0.11f, height * 0.08f),
                cornerRadius = CornerRadius(height * 0.04f, height * 0.04f)
            )
        }
    }
}

@Composable
fun InsuranceCardNotepadIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(52.dp)) {
        val width = size.width
        val height = size.height
        val padLeft = width * 0.18f
        val padTop = height * 0.08f
        val padWidth = width * 0.68f
        val padHeight = height * 0.84f

        drawRoundRect(
            color = NotepadPaper,
            topLeft = Offset(padLeft, padTop),
            size = Size(padWidth, padHeight),
            cornerRadius = CornerRadius(width * 0.04f, width * 0.04f)
        )
        drawRoundRect(
            color = Color(0x33000000),
            topLeft = Offset(padLeft, padTop),
            size = Size(padWidth, padHeight),
            cornerRadius = CornerRadius(width * 0.04f, width * 0.04f),
            style = Stroke(width = width * 0.02f)
        )

        repeat(4) { index ->
            val y = padTop + padHeight * 0.22f + index * padHeight * 0.14f
            drawLine(
                color = NotepadLine,
                start = Offset(padLeft + width * 0.08f, y),
                end = Offset(padLeft + padWidth - width * 0.06f, y),
                strokeWidth = width * 0.015f
            )
        }

        val spiralX = padLeft + width * 0.05f
        repeat(5) { index ->
            val y = padTop + padHeight * 0.14f + index * padHeight * 0.14f
            drawArc(
                color = NotepadSpiral,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(spiralX - width * 0.03f, y - height * 0.02f),
                size = Size(width * 0.06f, height * 0.04f),
                style = Stroke(width = width * 0.018f)
            )
        }

        val pencilPath = Path().apply {
            moveTo(padLeft + padWidth * 0.55f, padTop + padHeight * 0.62f)
            lineTo(padLeft + padWidth * 0.88f, padTop + padHeight * 0.28f)
            lineTo(padLeft + padWidth * 0.94f, padTop + padHeight * 0.34f)
            lineTo(padLeft + padWidth * 0.61f, padTop + padHeight * 0.68f)
            close()
        }
        drawPath(pencilPath, color = Color(0xFFFFB300))
        drawCircle(
            color = Color(0xFF5D4037),
            radius = width * 0.018f,
            center = Offset(padLeft + padWidth * 0.91f, padTop + padHeight * 0.31f)
        )
    }
}
