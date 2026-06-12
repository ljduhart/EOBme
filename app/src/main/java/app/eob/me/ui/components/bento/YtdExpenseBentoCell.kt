package app.eob.me.ui.components.bento

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.YtdBentoViewMode
import app.eob.me.data.YtdDeductibleBentoSnapshot
import app.eob.me.navigation.HubBentoDestination
import kotlin.random.Random

import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandGlow
import app.eob.me.ui.theme.EobLineTrajectory
import app.eob.me.ui.theme.EobParticleColor

private val LineBilled = EobBrandBlue
private val LinePatient = EobBrandGlow
private val LineTrajectory = EobLineTrajectory
private val ParticleColor = EobParticleColor

@Composable
fun YtdExpenseBentoCell(
    language: AppLanguage,
    snapshot: YtdDeductibleBentoSnapshot,
    viewMode: YtdBentoViewMode,
    onViewModeSelected: (YtdBentoViewMode) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val morph by animateFloatAsState(
        targetValue = if (viewMode == YtdBentoViewMode.DeductibleTracker) 1f else 0f,
        animationSpec = tween(650),
        label = "ytdViewMorph"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BentoCellLayout.ASPECT_RATIO),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = HubBentoDestination.YtdExpense.title(language),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                FilterChip(
                    selected = viewMode == YtdBentoViewMode.CostOverview,
                    onClick = { onViewModeSelected(YtdBentoViewMode.CostOverview) },
                    label = {
                        Text(
                            EobStrings.t(language, "ytdViewCost"),
                            fontSize = 7.sp,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.weight(1f),
                    border = null
                )
                FilterChip(
                    selected = viewMode == YtdBentoViewMode.DeductibleTracker,
                    onClick = { onViewModeSelected(YtdBentoViewMode.DeductibleTracker) },
                    label = {
                        Text(
                            EobStrings.t(language, "ytdViewDeductible"),
                            fontSize = 7.sp,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.weight(1f),
                    border = null
                )
            }
            Text(
                text = summaryLine(language, snapshot, morph),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 7.sp,
                lineHeight = 9.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            if (snapshot.onTrackEarlyDeductible && morph > 0.5f) {
                Text(
                    text = EobStrings.t(language, "ytdOnTrackEarly"),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 6.sp,
                    color = LineTrajectory,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                YtdInteractiveChart(
                    snapshot = snapshot,
                    morph = morph,
                    velocity = snapshot.spendingVelocity,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun YtdInteractiveChart(
    snapshot: YtdDeductibleBentoSnapshot,
    morph: Float,
    velocity: Float,
    modifier: Modifier = Modifier
) {
    val particleShift by rememberInfiniteTransition(label = "ytdParticles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2200 / velocity.coerceIn(0.25f, 1f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleFlow"
    )
    val particles = remember(snapshot.year) {
        List(18) {
            ParticleSeed(
                yRatio = Random.nextFloat() * 0.75f + 0.1f,
                radiusPx = Random.nextFloat() * 2f + 1f,
                phase = Random.nextFloat()
            )
        }
    }

    Canvas(modifier = modifier) {
        val primarySeries = lerpSeries(snapshot.monthlyBilledNormalized, snapshot.trajectoryNormalized, morph)
        val secondarySeries = snapshot.trajectoryNormalized

        drawSeries(primarySeries, LineBilled, morph)
        drawSeries(secondarySeries, LineTrajectory, morph * 0.85f + 0.15f)

        val patientFlat = List(12) { snapshot.outOfPocketProgress.coerceIn(0f, 1f) * 0.85f }
        drawSeries(
            lerpSeries(snapshot.monthlyBilledNormalized.map { it * 0.65f }, patientFlat, morph),
            LinePatient,
            1f - morph * 0.35f
        )

        particles.forEach { seed ->
            val x = (particleShift + seed.phase) % 1f * size.width
            val y = size.height * seed.yRatio
            drawCircle(
                color = ParticleColor.copy(alpha = 0.35f + morph * 0.25f),
                radius = seed.radiusPx,
                center = Offset(x, y)
            )
        }
    }
}

private data class ParticleSeed(val yRatio: Float, val radiusPx: Float, val phase: Float)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeries(
    points: List<Float>,
    color: Color,
    alphaScale: Float
) {
    if (points.isEmpty()) return
    val path = Path()
    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
    points.forEachIndexed { index, value ->
        val x = index * stepX
        val y = size.height * (1f - value.coerceIn(0f, 1.15f) / 1.15f)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path = path,
        color = color.copy(alpha = (0.55f + alphaScale * 0.4f).coerceIn(0.2f, 1f)),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun lerpSeries(a: List<Float>, b: List<Float>, t: Float): List<Float> {
    val size = maxOf(a.size, b.size, 12)
    return (0 until size).map { index ->
        val av = a.getOrElse(index) { a.lastOrNull() ?: 0f }
        val bv = b.getOrElse(index) { b.lastOrNull() ?: 0f }
        av + (bv - av) * t
    }
}

private fun summaryLine(language: AppLanguage, snapshot: YtdDeductibleBentoSnapshot, morph: Float): String {
    return if (morph < 0.5f) {
        EobStrings.tf(
            language,
            "ytdBilledVsResp",
            snapshot.totalBilled,
            snapshot.totalPatientResponsibility
        )
    } else {
        val deductibleLine = EobStrings.tf(
            language,
            "ytdDeductibleProgress",
            snapshot.deductiblePaidYtd,
            snapshot.deductibleLimit
        )
        val copayLine = EobStrings.tf(
            language,
            "ytdCopayCoinsurance",
            snapshot.copayPaidYtd,
            snapshot.coinsurancePaidYtd
        )
        "$deductibleLine · $copayLine"
    }
}
