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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.InsuranceNewsBentoSnapshot
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.ui.components.glassEffect
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandCyan
import app.eob.me.ui.theme.EobChartOrange
import app.eob.me.ui.theme.EobCyberSurfaceVariant
import kotlin.math.roundToInt

private val TickerAccent = EobBrandBlue
private val CriticalGlow = EobChartOrange
private val MicroCardGlow = EobBrandCyan

@Composable
fun InsuranceNewsBentoCell(
    language: AppLanguage,
    snapshot: InsuranceNewsBentoSnapshot,
    onClick: () -> Unit,
    cellAspectRatio: Float = BentoCellLayout.ASPECT_RATIO,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val flipRotation by animateFloatAsState(
        targetValue = if (snapshot.criticalAlertActive) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "insuranceNewsFlip"
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HubBentoIcon(
                    destination = HubBentoDestination.InsuranceNews,
                    tint = if (snapshot.criticalAlertActive) CriticalGlow else TickerAccent,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Text(
                    text = EobStrings.t(language, "bentoInsuranceNews"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NewsHeadlineTicker(
                    headlines = snapshot.tickerHeadlines,
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                )
                Box(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            rotationY = flipRotation
                            cameraDistance = 8f * density.density
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (flipRotation <= 90f) {
                        NewsMicroCardPreview(
                            language = language,
                            headline = snapshot.previewHeadline,
                            company = snapshot.previewCompany,
                            criticalActive = snapshot.criticalAlertActive
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                        ) {
                            NewsMicroCardActionFace(
                                language = language,
                                actionSummary = snapshot.actionSummary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsHeadlineTicker(
    headlines: List<String>,
    modifier: Modifier = Modifier
) {
    val loopHeadlines = remember(headlines) {
        if (headlines.isEmpty()) {
            listOf("")
        } else {
            headlines + headlines
        }
    }
    val lineHeightDp = 14.dp
    val cycleHeightPx = with(LocalDensity.current) { (lineHeightDp * loopHeadlines.size).toPx() }
    val halfCyclePx = cycleHeightPx / 2f

    val infiniteTransition = rememberInfiniteTransition(label = "newsTickerScroll")
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -halfCyclePx,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (loopHeadlines.size.coerceAtLeast(2) * 2200),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "newsTickerOffset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EobCyberSurfaceVariant)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, scrollOffset.roundToInt()) },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            loopHeadlines.forEach { headline ->
                Text(
                    text = headline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun NewsMicroCardPreview(
    language: AppLanguage,
    headline: String,
    company: String,
    criticalActive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassEffect(
                cornerRadius = 10.dp,
                glowColor = if (criticalActive) CriticalGlow else MicroCardGlow
            )
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = EobStrings.t(language, "newsBentoMicroPreview"),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
            color = TickerAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = headline,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                lineHeight = 10.sp
            ),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = company,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NewsMicroCardActionFace(
    language: AppLanguage,
    actionSummary: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassEffect(cornerRadius = 10.dp, glowColor = CriticalGlow)
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = EobStrings.t(language, "newsBentoCriticalAction"),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
            fontWeight = FontWeight.Bold,
            color = CriticalGlow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = actionSummary,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                lineHeight = 10.sp
            ),
            color = CriticalGlow.copy(alpha = 0.85f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = EobStrings.t(language, "newsBentoTapToOpen"),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
            fontWeight = FontWeight.SemiBold,
            color = TickerAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
