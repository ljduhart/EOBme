package app.eob.me.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import app.eob.me.ui.theme.EobCyberSurface
import app.eob.me.ui.theme.EobCyberSurfaceVariant

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")

    // Animate across a wider factor range to completely clear out edge components on wide layouts
    val translateAnimation by transition.animateFloat(
        initialValue = -2f * size.width.toFloat(),
        targetValue = 2f * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val shimmerColors = listOf(
        EobCyberSurface,
        EobCyberSurfaceVariant,
        EobCyberSurface
    )

    this.onGloballyPositioned { size = it.size }
        .background(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(x = translateAnimation, y = 0f),
                end = Offset(
                    x = translateAnimation + size.width.toFloat(),
                    y = size.composeOffsetFactor()
                )
            )
        )
}

// Inline extension layout to cleanly set a consistent diagonal slant angle
private fun IntSize.composeOffsetFactor(): Float = this.height.toFloat() * 0.45f
