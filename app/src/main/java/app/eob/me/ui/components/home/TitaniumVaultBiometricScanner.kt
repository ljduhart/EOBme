package app.eob.me.ui.components.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val TitaniumDark = Color(0xFF1A1D22)
private val TitaniumMid = Color(0xFF3B424C)
private val ScannerGlow = Color(0xFF7AD7FF)
private val UnlockGreen = Color(0xFF3DDC84)

@Composable
fun TitaniumVaultBiometricScanner(
    onVaultUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Long = 3_000L
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val ringProgress = remember { Animatable(0f) }
    val progress = ringProgress.value

    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(TitaniumMid, TitaniumDark)
                )
            )
            .pointerInput(holdDurationMs) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val job = scope.launch {
                            ringProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = holdDurationMs.toInt(),
                                    easing = FastOutSlowInEasing
                                )
                            )
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onVaultUnlocked()
                            ringProgress.snapTo(0f)
                        }
                        val released = tryAwaitRelease()
                        if (!released) {
                            job.cancel()
                            scope.launch { ringProgress.snapTo(0f) }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 5.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val ringColor = if (progress >= 0.99f) UnlockGreen else ScannerGlow
            drawArc(
                color = Color.White.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Icon(
            imageVector = Icons.Rounded.Fingerprint,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(30.dp)
        )
    }
}
