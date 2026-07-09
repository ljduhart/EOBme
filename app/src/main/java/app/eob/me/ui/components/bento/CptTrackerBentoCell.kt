package app.eob.me.ui.components.bento

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.CptBentoSnapshot
import app.eob.me.data.EobStrings
import app.eob.me.data.PriceTrendDirection
import app.eob.me.navigation.HubBentoDestination
import kotlinx.coroutines.delay

import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobGaugeHigh
import app.eob.me.ui.theme.EobGaugeLow
import app.eob.me.ui.theme.EobGaugeMid
import app.eob.me.ui.theme.EobGaugeTrack

private val RingBlue = EobBrandBlue
private val RingTrack = EobGaugeTrack
private val GaugeLow = EobGaugeLow
private val GaugeMid = EobGaugeMid
private val GaugeHigh = EobGaugeHigh

@Composable
fun CptTrackerBentoCell(
    language: AppLanguage,
    snapshot: CptBentoSnapshot,
    onClick: () -> Unit,
    cellAspectRatio: Float = BentoCellLayout.ASPECT_RATIO,
    modifier: Modifier = Modifier
) {
    var ringTarget by remember(snapshot.ringProgress) { mutableFloatStateOf(0f) }
    LaunchedEffect(snapshot.ringProgress) {
        ringTarget = 0f
        delay(280)
        ringTarget = snapshot.ringProgress
    }
    val animatedRing by animateFloatAsState(
        targetValue = ringTarget,
        animationSpec = tween(durationMillis = 1200),
        label = "cptRingDraw"
    )

    val trendLabel = when (snapshot.trendDirection) {
        PriceTrendDirection.BelowFair -> EobStrings.t(language, "cptPriceBelowFair")
        PriceTrendDirection.NearFair -> EobStrings.t(language, "cptPriceNearFair")
        PriceTrendDirection.AboveFair -> EobStrings.t(language, "cptPriceAboveFair")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cellAspectRatio)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            BentoCellTitle(
                text = HubBentoDestination.CptTracker.title(language),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    DelayedProgressRing(progress = animatedRing)
                    Text(
                        text = "${(animatedRing * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = RingBlue
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = snapshot.translatorLine,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 7.sp,
                        lineHeight = 9.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PriceRangeGauge(
                        position = snapshot.priceGaugePosition,
                        trendPoints = snapshot.priceTrendPoints,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = trendLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 6.sp,
                        color = gaugeColor(snapshot.priceGaugePosition),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DelayedProgressRing(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 4.dp.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = RingTrack,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = RingBlue,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PriceRangeGauge(
    position: Float,
    trendPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    val indicator by animateFloatAsState(
        targetValue = position,
        animationSpec = tween(900),
        label = "priceGaugeIndicator"
    )
    Canvas(modifier = modifier) {
        val barTop = size.height * 0.55f
        val barHeight = size.height * 0.2f
        drawRoundRect(
            color = RingTrack,
            topLeft = Offset(0f, barTop),
            size = Size(size.width, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2f)
        )
        val markerX = size.width * indicator.coerceIn(0.05f, 0.95f)
        drawLine(
            color = gaugeColor(indicator),
            start = Offset(markerX, barTop - 2f),
            end = Offset(markerX, barTop + barHeight + 2f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        if (trendPoints.size > 1) {
            val stepX = size.width / (trendPoints.size - 1).coerceAtLeast(1)
            var previous: Offset? = null
            trendPoints.forEachIndexed { index, value ->
                val point = Offset(
                    x = index * stepX,
                    y = size.height * (1f - value.coerceIn(0.05f, 0.95f))
                )
                previous?.let { prior ->
                    drawLine(
                        color = RingBlue.copy(alpha = 0.75f),
                        start = prior,
                        end = point,
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )
                }
                previous = point
            }
        }
    }
}

private fun gaugeColor(position: Float): Color = when {
    position < 0.4f -> GaugeLow
    position > 0.65f -> GaugeHigh
    else -> GaugeMid
}
