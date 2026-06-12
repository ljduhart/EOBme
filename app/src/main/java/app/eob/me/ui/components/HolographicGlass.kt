package app.eob.me.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandCyan
import app.eob.me.ui.theme.EobCyberGlassFill

/**
 * A highly reusable modifier extension that applies a glass blur fill
 * and an ultra-fine, neon-glowing cybernetic border layout.
 */
fun Modifier.glassEffect(
    cornerRadius: Dp = 16.dp,
    glowColor: Color = EobBrandCyan
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(EobCyberGlassFill)
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.4f),
                EobBrandBlue.copy(alpha = 0.1f),
                glowColor.copy(alpha = 0.05f),
                EobBrandBlue.copy(alpha = 0.3f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/** Alias for glass styling used across dashboard surfaces. */
fun Modifier.glassBackground(
    cornerRadius: Dp = 16.dp,
    glowColor: Color = EobBrandCyan
): Modifier = glassEffect(cornerRadius = cornerRadius, glowColor = glowColor)

/**
 * Premium glass container surface that paints an ambient radial neon aura behind
 * itself to elevate the spatial depth layout of your metrics cards.
 */
@Composable
fun HolographicGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glowColor: Color = EobBrandCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                val shadowPaint = Paint().asFrameworkPaint().apply {
                    color = glowColor.copy(alpha = 0.08f).toArgb()
                    setShadowLayer(
                        30.dp.toPx(),
                        0f,
                        4.dp.toPx(),
                        glowColor.copy(alpha = 0.12f).toArgb()
                    )
                }

                drawContext.canvas.nativeCanvas.drawRoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    cornerRadius.toPx(),
                    cornerRadius.toPx(),
                    shadowPaint
                )
            }
            .glassEffect(cornerRadius = cornerRadius, glowColor = glowColor)
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}
