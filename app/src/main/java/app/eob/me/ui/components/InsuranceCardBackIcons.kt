package app.eob.me.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import android.graphics.Typeface
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings

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

private val DxBentoMidnight = Color(0xFF001A3F)
private val DxBentoMidnightLight = Color(0xFF0D2B52)
private val DxNeonCyan = Color(0xFF00E5FF)
private val DxNeonCyanSoft = Color(0x664FC3F7)
private val DxSilver = Color(0xFFE0E6EE)
private val DxSilverEdge = Color(0xFFB8C4D4)
private val DxSilverDark = Color(0xFF5A6578)
private val DxCrossFill = Color(0xFF0A2540)

@Composable
fun InsuranceCardDxReverseLookupIcon(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 88.dp, height = 80.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = DxNeonCyan,
                    spotColor = DxNeonCyan
                )
        ) {
            val width = size.width
            val height = size.height
            val corner = width * 0.16f
            val inset = width * 0.05f
            val cardLeft = inset
            val cardTop = inset * 0.6f
            val cardWidth = width - inset * 2f
            val cardHeight = height - inset * 1.8f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(DxNeonCyanSoft, Color.Transparent),
                    startY = cardTop + cardHeight * 0.55f,
                    endY = cardTop + cardHeight + width * 0.12f
                ),
                topLeft = Offset(cardLeft - width * 0.04f, cardTop + cardHeight * 0.4f),
                size = Size(cardWidth + width * 0.08f, width * 0.2f),
                cornerRadius = CornerRadius(corner, corner)
            )

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(DxBentoMidnightLight, DxBentoMidnight),
                    center = Offset(width * 0.5f, cardTop + cardHeight * 0.35f),
                    radius = width * 0.85f
                ),
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                color = DxSilverEdge,
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = width * 0.022f)
            )
            drawRoundRect(
                color = DxSilver,
                topLeft = Offset(cardLeft + width * 0.012f, cardTop + width * 0.012f),
                size = Size(cardWidth - width * 0.024f, cardHeight - width * 0.024f),
                cornerRadius = CornerRadius(corner * 0.9f, corner * 0.9f),
                style = Stroke(width = width * 0.01f)
            )

            val crossLeft = cardLeft + cardWidth * 0.08f
            val crossTop = cardTop + cardHeight * 0.14f
            val crossSize = cardWidth * 0.26f
            drawRoundRect(
                color = DxSilver,
                topLeft = Offset(crossLeft, crossTop),
                size = Size(crossSize, crossSize),
                cornerRadius = CornerRadius(width * 0.025f, width * 0.025f)
            )
            drawRoundRect(
                color = DxCrossFill,
                topLeft = Offset(crossLeft + crossSize * 0.3f, crossTop + crossSize * 0.1f),
                size = Size(crossSize * 0.4f, crossSize * 0.8f),
                cornerRadius = CornerRadius(width * 0.015f, width * 0.015f)
            )
            drawRoundRect(
                color = DxCrossFill,
                topLeft = Offset(crossLeft + crossSize * 0.1f, crossTop + crossSize * 0.3f),
                size = Size(crossSize * 0.8f, crossSize * 0.4f),
                cornerRadius = CornerRadius(width * 0.015f, width * 0.015f)
            )
            drawDxIconLabel(
                text = "DX",
                centerX = crossLeft + crossSize * 0.5f,
                centerY = crossTop + crossSize * 0.58f,
                textSizePx = crossSize * 0.28f,
                color = DxCrossFill,
                bold = true
            )

            val gearOne = Offset(cardLeft + cardWidth * 0.44f, cardTop + cardHeight * 0.52f)
            val gearTwo = Offset(cardLeft + cardWidth * 0.54f, cardTop + cardHeight * 0.46f)
            drawNeonGear(center = gearOne, radius = width * 0.055f)
            drawNeonGear(center = gearTwo, radius = width * 0.042f)

            val circuitPath = Path().apply {
                moveTo(crossLeft + crossSize, crossTop + crossSize * 0.5f)
                lineTo(gearOne.x - width * 0.04f, gearOne.y)
                lineTo(gearTwo.x + width * 0.03f, gearTwo.y)
                lineTo(cardLeft + cardWidth * 0.62f, cardTop + cardHeight * 0.38f)
            }
            drawPath(
                path = circuitPath,
                color = DxNeonCyan,
                style = Stroke(width = width * 0.012f)
            )
            repeat(3) { index ->
                val nodeX = cardLeft + cardWidth * (0.58f + index * 0.06f)
                val nodeY = cardTop + cardHeight * 0.36f
                drawCircle(color = DxNeonCyan, radius = width * 0.012f, center = Offset(nodeX, nodeY))
            }

            val cptLeft = cardLeft + cardWidth * 0.58f
            val cptTop = cardTop + cardHeight * 0.12f
            drawDxIconLabel(
                text = "CPT",
                centerX = cptLeft + cardWidth * 0.2f,
                centerY = cptTop + cardHeight * 0.1f,
                textSizePx = width * 0.065f,
                color = DxSilver,
                bold = true
            )
            drawDxIconLabel(
                text = "[9|9|2|1|4]",
                centerX = cptLeft + cardWidth * 0.2f,
                centerY = cptTop + cardHeight * 0.22f,
                textSizePx = width * 0.042f,
                color = DxSilver,
                bold = false
            )

            val gridTop = cptTop + cardHeight * 0.28f
            val bentoColors = listOf(
                Color(0xFFE57373),
                Color(0xFF81C784),
                Color(0xFF64B5F6),
                Color(0xFFFFB74D),
                Color(0xFFBA68C8),
                Color(0xFF4DD0E1)
            )
            repeat(4) { row ->
                repeat(4) { col ->
                    val cell = width * 0.038f
                    val x = cptLeft + col * (cell + width * 0.008f)
                    val y = gridTop + row * (cell + width * 0.008f)
                    drawRoundRect(
                        color = bentoColors[(row + col) % bentoColors.size],
                        topLeft = Offset(x, y),
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(width * 0.008f, width * 0.008f)
                    )
                }
            }
        }
        Text(
            text = EobStrings.t(language, "insuranceCardReverseDxProcessingCaption"),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                fontSize = 9.sp
            ),
            color = DxNeonCyan,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun DrawScope.drawDxIconLabel(
    text: String,
    centerX: Float,
    centerY: Float,
    textSizePx: Float,
    color: Color,
    bold: Boolean
) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            this.color = color.toArgb()
            this.textSize = textSizePx
            this.textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = if (bold) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else {
                Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
        }
        drawText(text, centerX, centerY, paint)
    }
}

private fun DrawScope.drawNeonGear(center: Offset, radius: Float) {
    drawCircle(color = DxNeonCyan.copy(alpha = 0.35f), radius = radius * 1.35f, center = center)
    drawCircle(color = DxSilver, radius = radius, center = center)
    repeat(6) { index ->
        rotate(degrees = index * 60f, pivot = center) {
            drawRoundRect(
                color = DxNeonCyan,
                topLeft = Offset(center.x - radius * 0.22f, center.y - radius * 1.45f),
                size = Size(radius * 0.44f, radius * 0.55f),
                cornerRadius = CornerRadius(radius * 0.12f, radius * 0.12f)
            )
        }
    }
    drawCircle(color = DxBentoMidnight, radius = radius * 0.38f, center = center)
}
