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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.HistoryBentoSnapshot
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.data.TaxVaultBudgetSummary
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.asCurrency
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandGlow
import app.eob.me.ui.theme.EobChartIndigo
import app.eob.me.ui.theme.EobCyberTextPrimary
import kotlinx.coroutines.delay

private val GlowBlue = EobBrandBlue
private val GlowCyan = EobBrandGlow

@Composable
fun HistoryBentoCell(
    language: AppLanguage,
    snapshot: HistoryBentoSnapshot,
    taxVaultActive: Boolean = false,
    taxVaultBudgetSummary: TaxVaultBudgetSummary = TaxVaultBudgetSummary(0.0, 0.0),
    taxVaultFilterState: TaxVaultFilterState = TaxVaultFilterState.OFF,
    processingPhase: InvoiceProcessingPhase,
    isLoadingInvoice: Boolean,
    activeFilter: HistoryBentoFilter,
    billingErrorDetectionEnabled: Boolean = true,
    onClick: () -> Unit,
    onFilterSelected: (HistoryBentoFilter) -> Unit,
    onFileDropAnimationFinished: () -> Unit,
    cellAspectRatio: Float = BentoCellLayout.ASPECT_RATIO,
    modifier: Modifier = Modifier
) {
    val isPulsing = isLoadingInvoice || processingPhase == InvoiceProcessingPhase.Processing
    val showFileDrop = processingPhase == InvoiceProcessingPhase.FileDropReveal

    LaunchedEffect(showFileDrop) {
        if (showFileDrop) {
            delay(950)
            onFileDropAnimationFinished()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "historyBentoPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "historyPulseAlpha"
    )

    val dropProgress by animateFloatAsState(
        targetValue = if (showFileDrop) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fileDropProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cellAspectRatio)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isPulsing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GlowCyan.copy(alpha = pulseAlpha * 0.45f),
                                    GlowBlue.copy(alpha = pulseAlpha * 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (taxVaultActive) {
                        EobStrings.t(language, "taxVaultSnapshotTitle")
                    } else {
                        HubBentoDestination.EobHistory.title(language)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (taxVaultActive) Color(0xFF3DDC84) else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (taxVaultActive) {
                    Text(
                        text = EobStrings.t(language, "taxVaultFilteredBanner"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 7.sp,
                        color = Color(0xFF3DDC84).copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFF3DDC84).copy(alpha = 0.12f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(vertical = 2.dp)
                    )
                }

                if (!taxVaultActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    HistoryFilterBadge(
                        language = language,
                        label = EobStrings.t(language, "historyFilterAll"),
                        count = null,
                        selected = activeFilter == HistoryBentoFilter.All,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = { onFilterSelected(HistoryBentoFilter.All) },
                        modifier = Modifier.weight(1f)
                    )
                    if (billingErrorDetectionEnabled) {
                        HistoryFilterBadge(
                            language = language,
                            label = EobStrings.t(language, "historyFilterFlagged"),
                            count = snapshot.flaggedBillingErrorCount,
                            selected = activeFilter == HistoryBentoFilter.Flagged,
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { onFilterSelected(HistoryBentoFilter.Flagged) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                }

                if (taxVaultActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                    ) {
                        VaultSnapshotMetric(
                            label = EobStrings.t(language, "taxVaultTotalEligible"),
                            value = taxVaultBudgetSummary.eligibleAmount.asCurrency()
                        )
                        VaultSnapshotMetric(
                            label = EobStrings.t(language, "taxVaultTotalSaved"),
                            value = taxVaultBudgetSummary.savedAmount.asCurrency()
                        )
                    }
                } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CornerstoneGrid(
                        quadrantStrengths = snapshot.cornerstoneQuadrants,
                        modifier = Modifier
                            .weight(0.42f)
                            .aspectRatio(1f)
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = EobStrings.t(language, "historySpendingPreview"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 7.sp,
                            color = GlowBlue,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SpendingGlowChart(
                            monthlySpend = snapshot.monthlySpend,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
                }
            }

            if (showFileDrop && dropProgress > 0f) {
                FileDropOverlay(
                    progress = dropProgress,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun VaultSnapshotMetric(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3DDC84).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 7.sp,
            color = GlowBlue,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3DDC84),
            maxLines = 1
        )
    }
}

@Composable
private fun HistoryFilterBadge(
    language: AppLanguage,
    label: String,
    count: Int?,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = if (count != null && count > 0) {
                    EobStrings.tf(language, "historyFilterCount", count, label)
                } else {
                    label
                },
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                maxLines = 1
            )
        },
        modifier = modifier.height(24.dp),
        border = null,
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = tint.copy(alpha = 0.18f),
            selectedLabelColor = tint
        )
    )
}

@Composable
private fun CornerstoneGrid(
    quadrantStrengths: List<Float>,
    modifier: Modifier = Modifier
) {
    val strengths = quadrantStrengths.take(4).ifEmpty { List(4) { 0.3f } }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CornerstoneBlock(strengths[0], Modifier.weight(1f))
            CornerstoneBlock(strengths.getOrElse(1) { 0.3f }, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CornerstoneBlock(strengths.getOrElse(2) { 0.3f }, Modifier.weight(1f))
            CornerstoneBlock(strengths.getOrElse(3) { 0.3f }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CornerstoneBlock(strength: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        GlowBlue.copy(alpha = 0.25f + strength * 0.45f),
                        GlowCyan.copy(alpha = 0.15f + strength * 0.35f)
                    )
                ),
                RoundedCornerShape(4.dp)
            )
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun SpendingGlowChart(
    monthlySpend: List<Double>,
    modifier: Modifier = Modifier
) {
    val values = monthlySpend.ifEmpty { listOf(0.0, 0.0, 0.0) }
    val maxValue = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Canvas(modifier = modifier) {
        val barCount = values.size
        val gap = size.width * 0.08f
        val barWidth = (size.width - gap * (barCount + 1)) / barCount
        values.forEachIndexed { index, amount ->
            val fraction = (amount / maxValue).toFloat().coerceIn(0.08f, 1f)
            val barHeight = size.height * 0.82f * fraction
            val left = gap + index * (barWidth + gap)
            val top = size.height * 0.9f - barHeight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(GlowCyan, GlowBlue),
                    startY = top,
                    endY = top + barHeight
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth * 0.25f)
            )
            drawRoundRect(
                color = EobCyberTextPrimary.copy(alpha = 0.35f),
                topLeft = Offset(left + 1f, top + 1f),
                size = Size(barWidth - 2f, barHeight * 0.35f),
                cornerRadius = CornerRadius(barWidth * 0.2f)
            )
        }
        drawLine(
            color = GlowCyan.copy(alpha = 0.55f),
            start = Offset(0f, size.height * 0.92f),
            end = Offset(size.width, size.height * 0.92f),
            strokeWidth = 1.5f
        )
    }
}

@Composable
private fun FileDropOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val docOffsetY = (-36).dp + (48.dp * progress)
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f * progress)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .offset(y = docOffsetY)
                    .size(width = 28.dp, height = 34.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(3.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GlowBlue.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(width = 36.dp, height = 10.dp)
                    .background(EobChartIndigo, RoundedCornerShape(2.dp))
            )
        }
    }
}
