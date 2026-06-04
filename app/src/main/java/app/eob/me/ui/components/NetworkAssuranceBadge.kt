package app.eob.me.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.NetworkAssuranceState

val AssuranceCyan = Color(0xFF00E5FF)
val AssuranceGold = Color(0xFFD4AF37)
val AssuranceCrimson = Color(0xFFEF4444)

@Composable
fun NetworkAssuranceBadge(
    state: NetworkAssuranceState,
    statusLabel: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = when (state) {
        NetworkAssuranceState.FullyAssured -> AssuranceCyan
        NetworkAssuranceState.VerificationPending -> AssuranceGold
        NetworkAssuranceState.OutOfNetworkAlert -> AssuranceCrimson
    }

    Box(modifier = modifier) {
        if (state == NetworkAssuranceState.FullyAssured) {
            AssuranceBreathingPulse(modifier = Modifier.matchParentSize())
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 6.dp else 8.dp)
                    .background(accent, CircleShape)
            )
            if (!compact) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(start = 4.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun NetworkAssuranceWarningDot(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(AssuranceGold, CircleShape)
            .border(1.dp, Color.White, CircleShape)
    )
}

@Composable
private fun AssuranceBreathingPulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "assurancePulse")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "assurancePulseAlpha"
    )
    Canvas(modifier = modifier) {
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(AssuranceCyan.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.65f
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(12.dp.toPx())
        )
    }
}
