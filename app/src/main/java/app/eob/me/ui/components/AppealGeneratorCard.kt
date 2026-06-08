package app.eob.me.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppealGeneratorSnapshot
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import kotlinx.coroutines.delay

private val ShieldBlue = Color(0xFF2498EA)
private val ShieldCyan = Color(0xFF00E5FF)
private val ShieldGold = Color(0xFFD4AF37)

@Composable
fun AppealGeneratorCard(
    language: AppLanguage,
    snapshot: AppealGeneratorSnapshot,
    onOpenAppeal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var breatheExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            breatheExpanded = !breatheExpanded
            delay(2_400)
        }
    }
    val breathScale by animateFloatAsState(
        targetValue = if (breatheExpanded) 1.04f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "appealShieldScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "appealShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (snapshot.claimScanComplete) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "appealShimmerOffset"
    )

    val settledShimmer by animateFloatAsState(
        targetValue = if (snapshot.claimScanComplete) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "appealShimmerSettle"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2498EA).copy(alpha = 0.15f),
            ShieldCyan.copy(alpha = 0.85f),
            ShieldGold.copy(alpha = 0.65f),
            Color(0xFF2498EA).copy(alpha = 0.15f)
        ),
        start = Offset(200f * shimmerOffset, 0f),
        end = Offset(200f * shimmerOffset + 260f, 40f)
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenAppeal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier.size(52.dp)
            ) {
                scale(breathScale, breathScale, center) {
                    val shieldPath = Path().apply {
                        val w = size.width
                        val h = size.height
                        moveTo(w * 0.5f, h * 0.06f)
                        cubicTo(w * 0.82f, h * 0.16f, w * 0.9f, h * 0.42f, w * 0.5f, h * 0.94f)
                        cubicTo(w * 0.1f, h * 0.42f, w * 0.18f, h * 0.16f, w * 0.5f, h * 0.06f)
                        close()
                    }
                    drawPath(
                        path = shieldPath,
                        brush = Brush.linearGradient(
                            colors = listOf(ShieldCyan, ShieldBlue, ShieldGold.copy(alpha = 0.8f))
                        )
                    )
                    drawPath(
                        path = shieldPath,
                        color = Color.White.copy(alpha = 0.35f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = EobStrings.t(language, "appealGeneratorTitle"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                HolographicAppealLine(
                    text = snapshot.statusLine,
                    shimmerBrush = shimmerBrush,
                    settled = settledShimmer,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                HolographicAppealLine(
                    text = snapshot.summaryLine,
                    shimmerBrush = shimmerBrush,
                    settled = settledShimmer,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = EobStrings.t(language, "appealGeneratorTapToOpen"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HolographicAppealLine(
    text: String,
    shimmerBrush: Brush,
    settled: Float,
    style: TextStyle
) {
    Text(
        text = text,
        style = if (settled >= 0.98f) {
            style.copy(color = MaterialTheme.colorScheme.onSurface)
        } else {
            style.copy(brush = shimmerBrush)
        }
    )
}
