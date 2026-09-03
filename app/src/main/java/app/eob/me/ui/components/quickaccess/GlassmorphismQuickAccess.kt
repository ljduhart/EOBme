package app.eob.me.ui.components.quickaccess

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings

private val GlassPaneShape = RoundedCornerShape(22.dp)
private val GlassPaneFill = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.20f),
        Color.White.copy(alpha = 0.11f),
        Color.White.copy(alpha = 0.16f)
    )
)
private val GlassPaneBorder = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.62f),
        Color.White.copy(alpha = 0.14f),
        Color.White.copy(alpha = 0.38f),
        Color.White.copy(alpha = 0.10f)
    ),
    start = Offset(0f, 0f),
    end = Offset(600f, 900f)
)
private val GlassTitleColor = Color(0xFFF5F9FF)
private val GlassLabelColor = Color(0xFFFFFFFF)
private val CyanGlow = Color(0xFF00E5FF)
private val ElectricBlue = Color(0xFF4FC3F7)
private val AmberGlass = Color(0xFFF9A825)
private val AmberGlassDeep = Color(0xFFE65100)
private val SilverMetal = Color(0xFFE8EEF5)
private val SilverMetalDark = Color(0xFF90A4AE)

@Composable
fun GlassmorphismQuickAccessHub(
    language: AppLanguage,
    insuranceCardBackIconsBlurred: Boolean,
    onOpenSmartRxVault: () -> Unit,
    onOpenClinicalNotes: () -> Unit,
    onOpenReverseDxLookup: () -> Unit,
    onOpenMedicalDictionary: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicQuickAccessPane(
        modifier = modifier,
        title = EobStrings.t(language, "insuranceCardBackHubTitle")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            GlassQuickActionTile(
                label = EobStrings.t(language, "insuranceCardMedicationsLauncher"),
                contentDescription = EobStrings.t(language, "insuranceCardMedicationsLauncherDescription"),
                iconsBlurred = insuranceCardBackIconsBlurred,
                onClick = onOpenSmartRxVault,
                icon = { CanvasMedsIcon() }
            )
            GlassQuickActionTile(
                label = EobStrings.t(language, "insuranceCardNotepadLauncher"),
                contentDescription = EobStrings.t(language, "insuranceCardNotepadLauncherDescription"),
                iconsBlurred = insuranceCardBackIconsBlurred,
                onClick = onOpenClinicalNotes,
                icon = { CanvasNotepadIcon() }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            GlassQuickActionTile(
                label = EobStrings.t(language, "insuranceCardReverseDxLauncher"),
                contentDescription = EobStrings.t(language, "insuranceCardReverseDxLauncherDescription"),
                iconsBlurred = insuranceCardBackIconsBlurred,
                onClick = onOpenReverseDxLookup,
                icon = { CanvasDxCptIcon() }
            )
            GlassQuickActionTile(
                label = EobStrings.t(language, "insuranceCardMedicalDictionaryLauncher"),
                contentDescription = EobStrings.t(language, "insuranceCardMedicalDictionaryLauncherDescription"),
                iconsBlurred = insuranceCardBackIconsBlurred,
                onClick = onOpenMedicalDictionary,
                icon = { CanvasDictionaryIcon() }
            )
        }
    }
}

@Composable
private fun GlassmorphicQuickAccessPane(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(GlassPaneShape)
            .border(width = 1.dp, brush = GlassPaneBorder, shape = GlassPaneShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                }
                .background(GlassPaneFill)
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp)
        ) {
            val streakBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    CyanGlow.copy(alpha = 0.22f),
                    ElectricBlue.copy(alpha = 0.16f),
                    Color(0xFF7C4DFF).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                start = Offset(0f, size.height * 0.35f),
                end = Offset(size.width, size.height * 0.65f)
            )
            drawRect(brush = streakBrush)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyanGlow.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.22f),
                    radius = size.minDimension * 0.42f
                ),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.18f, size.height * 0.22f),
                blendMode = BlendMode.Screen
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7C4DFF).copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.78f),
                    radius = size.minDimension * 0.36f
                ),
                radius = size.minDimension * 0.36f,
                center = Offset(size.width * 0.82f, size.height * 0.78f),
                blendMode = BlendMode.Screen
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GlassTitleColor,
                modifier = Modifier.fillMaxWidth()
            )
            content()
        }
    }
}

@Composable
private fun GlassQuickActionTile(
    label: String,
    contentDescription: String,
    iconsBlurred: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 108.dp, max = 132.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(76.dp),
            contentAlignment = Alignment.Center
        ) {
            if (iconsBlurred) {
                Box(modifier = Modifier.blur(18.dp)) {
                    icon()
                }
            } else {
                icon()
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = GlassLabelColor,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun CanvasMedsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(76.dp)) {
        val width = size.width
        val height = size.height
        val bodyLeft = width * 0.24f
        val bodyRight = width * 0.76f
        val bodyTop = width * 0.30f
        val bodyBottom = height * 0.90f
        val capTop = height * 0.06f
        val capBottom = bodyTop + height * 0.02f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF5F5F5), Color(0xFFD9DEE5))
            ),
            topLeft = Offset(bodyLeft - width * 0.05f, capTop),
            size = Size(bodyRight - bodyLeft + width * 0.10f, capBottom - capTop),
            cornerRadius = CornerRadius(width * 0.05f, width * 0.05f)
        )
        repeat(6) { index ->
            val x = bodyLeft - width * 0.02f + index * ((bodyRight - bodyLeft + width * 0.04f) / 5f)
            drawLine(
                color = Color(0x33000000),
                start = Offset(x, capTop + height * 0.02f),
                end = Offset(x, capBottom - height * 0.01f),
                strokeWidth = width * 0.010f
            )
        }

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    AmberGlass.copy(alpha = 0.92f),
                    AmberGlassDeep.copy(alpha = 0.78f),
                    AmberGlass.copy(alpha = 0.88f)
                ),
                start = Offset(bodyLeft, bodyTop),
                end = Offset(bodyRight, bodyBottom)
            ),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(width * 0.10f, width * 0.10f)
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent, Color.Transparent)
            ),
            topLeft = Offset(bodyLeft + width * 0.03f, bodyTop),
            size = Size(width * 0.10f, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(width * 0.06f, width * 0.06f),
            blendMode = BlendMode.Screen
        )

        val labelTop = bodyTop + height * 0.10f
        val labelHeight = height * 0.30f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.94f),
            topLeft = Offset(bodyLeft + width * 0.08f, labelTop),
            size = Size(bodyRight - bodyLeft - width * 0.16f, labelHeight),
            cornerRadius = CornerRadius(width * 0.03f, width * 0.03f)
        )
        drawCanvasLabel(
            text = "MEDS",
            centerX = width / 2f,
            centerY = labelTop + labelHeight * 0.42f,
            textSizePx = width * 0.11f,
            color = Color(0xFF1565C0),
            bold = true
        )
        repeat(2) { index ->
            val lineY = labelTop + labelHeight * 0.62f + index * height * 0.05f
            drawLine(
                color = Color(0xFF90CAF9),
                start = Offset(bodyLeft + width * 0.14f, lineY),
                end = Offset(bodyRight - width * 0.14f, lineY),
                strokeWidth = width * 0.012f,
                cap = StrokeCap.Round
            )
        }

        val capsuleCenter = Offset(width / 2f, bodyBottom - height * 0.12f)
        rotate(degrees = -28f, pivot = capsuleCenter) {
            drawRoundRect(
                color = Color(0xFFE53935),
                topLeft = Offset(capsuleCenter.x - width * 0.12f, capsuleCenter.y - height * 0.035f),
                size = Size(width * 0.12f, height * 0.07f),
                cornerRadius = CornerRadius(height * 0.035f, height * 0.035f)
            )
            drawRoundRect(
                color = Color(0xFF1E88E5),
                topLeft = Offset(capsuleCenter.x, capsuleCenter.y - height * 0.035f),
                size = Size(width * 0.12f, height * 0.07f),
                cornerRadius = CornerRadius(height * 0.035f, height * 0.035f)
            )
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.28f),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(width * 0.10f, width * 0.10f),
            style = Stroke(width = width * 0.012f)
        )
    }
}

@Composable
fun CanvasNotepadIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(76.dp)) {
        val width = size.width
        val height = size.height
        val sheetOffsets = listOf(
            Offset(width * 0.04f, height * 0.06f),
            Offset(width * 0.02f, height * 0.03f),
            Offset(0f, 0f)
        )
        sheetOffsets.forEachIndexed { index, offset ->
            val padLeft = width * 0.16f + offset.x
            val padTop = height * 0.10f + offset.y
            val padWidth = width * 0.68f
            val padHeight = height * 0.78f
            val alpha = 0.22f + index * 0.10f
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha + 0.10f),
                        Color.White.copy(alpha = alpha)
                    ),
                    start = Offset(padLeft, padTop),
                    end = Offset(padLeft + padWidth, padTop + padHeight)
                ),
                topLeft = Offset(padLeft, padTop),
                size = Size(padWidth, padHeight),
                cornerRadius = CornerRadius(width * 0.04f, width * 0.04f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = Offset(padLeft, padTop),
                size = Size(padWidth, padHeight),
                cornerRadius = CornerRadius(width * 0.04f, width * 0.04f),
                style = Stroke(width = width * 0.012f)
            )
            if (index == sheetOffsets.lastIndex) {
                repeat(5) { lineIndex ->
                    val y = padTop + padHeight * 0.18f + lineIndex * padHeight * 0.13f
                    drawLine(
                        color = Color.White.copy(alpha = 0.42f),
                        start = Offset(padLeft + width * 0.08f, y),
                        end = Offset(padLeft + padWidth - width * 0.08f, y),
                        strokeWidth = width * 0.010f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        val penTip = Offset(width * 0.70f, height * 0.30f)
        val penTail = Offset(width * 0.42f, height * 0.72f)
        val penPath = Path().apply {
            moveTo(penTail.x - width * 0.03f, penTail.y - height * 0.02f)
            lineTo(penTip.x + width * 0.02f, penTip.y + height * 0.02f)
            lineTo(penTip.x + width * 0.05f, penTip.y - height * 0.01f)
            lineTo(penTail.x, penTail.y - height * 0.05f)
            close()
        }
        drawPath(
            path = penPath,
            brush = Brush.linearGradient(
                colors = listOf(SilverMetalDark, SilverMetal, SilverMetalDark),
                start = penTail,
                end = penTip
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyanGlow, CyanGlow.copy(alpha = 0.25f), Color.Transparent),
                center = penTip,
                radius = width * 0.10f
            ),
            radius = width * 0.10f,
            center = penTip,
            blendMode = BlendMode.Screen
        )
        drawCircle(
            color = CyanGlow,
            radius = width * 0.018f,
            center = penTip
        )
    }
}

@Composable
fun CanvasDxCptIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(76.dp)) {
        val width = size.width
        val height = size.height
        val center = Offset(width * 0.50f, height * 0.50f)
        val radius = width * 0.30f

        val facetPath = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius * 0.88f, center.y - radius * 0.18f)
            lineTo(center.x + radius * 0.62f, center.y + radius * 0.82f)
            lineTo(center.x - radius * 0.62f, center.y + radius * 0.82f)
            lineTo(center.x - radius * 0.88f, center.y - radius * 0.18f)
            close()
        }
        drawPath(
            path = facetPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0D2B52).copy(alpha = 0.88f),
                    Color(0xFF001A3F).copy(alpha = 0.72f),
                    Color(0xFF00112A).copy(alpha = 0.92f)
                ),
                center = center,
                radius = radius * 1.2f
            )
        )
        drawPath(
            path = facetPath,
            color = CyanGlow.copy(alpha = 0.55f),
            style = Stroke(width = width * 0.014f)
        )

        val innerFacet = Path().apply {
            moveTo(center.x, center.y - radius * 0.55f)
            lineTo(center.x + radius * 0.42f, center.y - radius * 0.05f)
            lineTo(center.x + radius * 0.28f, center.y + radius * 0.48f)
            lineTo(center.x - radius * 0.28f, center.y + radius * 0.48f)
            lineTo(center.x - radius * 0.42f, center.y - radius * 0.05f)
            close()
        }
        drawPath(
            path = innerFacet,
            brush = Brush.linearGradient(
                colors = listOf(
                    CyanGlow.copy(alpha = 0.18f),
                    Color.Transparent,
                    ElectricBlue.copy(alpha = 0.14f)
                ),
                start = Offset(center.x, center.y - radius * 0.55f),
                end = Offset(center.x, center.y + radius * 0.48f)
            ),
            blendMode = BlendMode.Screen
        )

        val nodeOffsets = listOf(
            Offset(center.x, center.y - radius * 0.18f),
            Offset(center.x - radius * 0.28f, center.y + radius * 0.12f),
            Offset(center.x + radius * 0.28f, center.y + radius * 0.12f),
            Offset(center.x, center.y + radius * 0.30f)
        )
        nodeOffsets.forEachIndexed { index, node ->
            val next = nodeOffsets[(index + 1) % nodeOffsets.size]
            drawLine(
                color = CyanGlow.copy(alpha = 0.45f),
                start = node,
                end = next,
                strokeWidth = width * 0.010f
            )
        }
        nodeOffsets.forEach { node ->
            drawCircle(color = CyanGlow, radius = width * 0.014f, center = node)
        }

        drawCanvasLabel(
            text = "DX",
            centerX = center.x - radius * 0.18f,
            centerY = center.y + radius * 0.02f,
            textSizePx = width * 0.11f,
            color = Color.White,
            bold = true
        )
        drawCanvasLabel(
            text = "CPT",
            centerX = center.x + radius * 0.20f,
            centerY = center.y + radius * 0.18f,
            textSizePx = width * 0.085f,
            color = Color.White,
            bold = true
        )
    }
}

@Composable
fun CanvasDictionaryIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(76.dp)) {
        val width = size.width
        val height = size.height
        val bookLeft = width * 0.14f
        val bookTop = height * 0.10f
        val bookWidth = width * 0.50f
        val bookHeight = height * 0.72f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.20f)
                ),
                start = Offset(bookLeft, bookTop),
                end = Offset(bookLeft + bookWidth, bookTop + bookHeight)
            ),
            topLeft = Offset(bookLeft, bookTop),
            size = Size(bookWidth, bookHeight),
            cornerRadius = CornerRadius(width * 0.04f, width * 0.04f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.50f),
            topLeft = Offset(bookLeft, bookTop),
            size = Size(bookWidth, bookHeight),
            cornerRadius = CornerRadius(width * 0.04f, width * 0.04f),
            style = Stroke(width = width * 0.012f)
        )
        drawCanvasLabel(
            text = "MEDICAL",
            centerX = bookLeft + bookWidth * 0.50f,
            centerY = bookTop + bookHeight * 0.22f,
            textSizePx = width * 0.055f,
            color = Color.White.copy(alpha = 0.92f),
            bold = true
        )
        drawCanvasLabel(
            text = "TERMS",
            centerX = bookLeft + bookWidth * 0.50f,
            centerY = bookTop + bookHeight * 0.32f,
            textSizePx = width * 0.055f,
            color = Color.White.copy(alpha = 0.92f),
            bold = true
        )
        repeat(4) { index ->
            val lineY = bookTop + bookHeight * (0.42f + index * 0.10f)
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(bookLeft + bookWidth * 0.14f, lineY),
                end = Offset(bookLeft + bookWidth * 0.86f, lineY),
                strokeWidth = width * 0.010f,
                cap = StrokeCap.Round
            )
        }

        val lensCenter = Offset(bookLeft + bookWidth * 0.78f, bookTop + bookHeight * 0.72f)
        val lensRadius = width * 0.17f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                center = lensCenter,
                radius = lensRadius
            ),
            radius = lensRadius,
            center = lensCenter
        )
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(SilverMetal, SilverMetalDark, SilverMetal)
            ),
            radius = lensRadius,
            center = lensCenter,
            style = Stroke(width = width * 0.030f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = lensRadius * 0.72f,
            center = Offset(lensCenter.x - lensRadius * 0.18f, lensCenter.y - lensRadius * 0.18f),
            style = Stroke(width = width * 0.010f)
        )
        drawLine(
            color = SilverMetal,
            start = Offset(lensCenter.x + lensRadius * 0.62f, lensCenter.y + lensRadius * 0.62f),
            end = Offset(lensCenter.x + lensRadius * 1.45f, lensCenter.y + lensRadius * 1.45f),
            strokeWidth = width * 0.028f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCanvasLabel(
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
            textAlign = Paint.Align.CENTER
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
