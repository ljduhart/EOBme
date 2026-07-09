package app.eob.me.ui.components.bento

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.navigation.HubBentoDestination
import kotlinx.coroutines.delay

import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandGlow

private val GlowCyan = EobBrandGlow
private val GlowBlue = EobBrandBlue

@Composable
fun AppealGeneratorBentoCell(
    language: AppLanguage,
    isProcessing: Boolean,
    onClick: () -> Unit,
    onProcessingAnimationFinished: () -> Unit,
    cellAspectRatio: Float = BentoCellLayout.ASPECT_RATIO,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            delay(900)
            onProcessingAnimationFinished()
        }
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isProcessing) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "appealBentoPressScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "appealBentoGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "appealBentoGlowAlpha"
    )

    val iconTint = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cellAspectRatio)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(enabled = !isProcessing, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GlowCyan.copy(alpha = glowAlpha * 0.55f),
                                    GlowBlue.copy(alpha = glowAlpha * 0.3f),
                                    Color.Transparent
                                ),
                                center = Offset.Unspecified,
                                radius = 280f
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isProcessing) {
                    AppealProcessingRing(tint = iconTint, modifier = Modifier.padding(bottom = 4.dp))
                } else {
                    HubBentoIcon(
                        destination = HubBentoDestination.AppealGenerator,
                        tint = iconTint,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                BentoCellTitle(
                    text = HubBentoDestination.AppealGenerator.title(language),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AppealProcessingRing(
    tint: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "appealProcessingRing")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "appealRingRotation"
    )

    Canvas(modifier = modifier.size(26.dp)) {
        val strokeWidth = size.minDimension * 0.09f
        val inset = size.minDimension * 0.14f
        val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
        rotate(rotation) {
            drawArc(
                color = tint,
                startAngle = 0f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = GlowCyan.copy(alpha = 0.45f),
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
            )
        }
    }
}
