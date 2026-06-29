package app.eob.me.ui.components.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.TaxVaultBudgetSummary
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.TaxVaultVisibilityMode
import app.eob.me.data.asCurrency
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandGlow
import app.eob.me.ui.theme.EobInsuranceGradientEnd
import app.eob.me.ui.theme.EobInsuranceGradientMid
import app.eob.me.ui.theme.EobInsuranceGradientStart
import kotlinx.coroutines.delay

private enum class VaultUiPhase {
    OFF,
    ACTIVATING,
    ON
}

private val VaultCardCornerRadius = 12.dp
private val VaultCardBackground = Brush.linearGradient(
    colors = listOf(
        EobInsuranceGradientStart,
        EobInsuranceGradientMid,
        EobInsuranceGradientEnd
    ),
    start = Offset(0f, 0f),
    end = Offset(900f, 1200f)
)
private val GlowGreen = Color(0xFF3DDC84)
private val VaultGoldText = Color(0xFFFFD166)
/** Matches care-team smart card primary ink ([HomeCareTeamCards] CardInk / [EobBrandBlue]). */
private val VaultNeonText = EobBrandBlue

@Composable
fun TaxVaultVerticalFilterCard(
    language: AppLanguage,
    darkModeEnabled: Boolean,
    isGoldTier: Boolean,
    filterState: TaxVaultFilterState,
    visibilityMode: TaxVaultVisibilityMode,
    budgetSummary: TaxVaultBudgetSummary,
    onFilterSelected: (TaxVaultFilterState) -> Unit,
    onVisibilityModeSelected: (TaxVaultVisibilityMode) -> Unit,
    onVaultDoorUnlocked: () -> Unit = {},
    showTitaniumDoor: Boolean = true,
    modifier: Modifier = Modifier
) {
    var uiPhase by remember { mutableStateOf(VaultUiPhase.OFF) }
    var rippleAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(filterState) {
        uiPhase = when (filterState) {
            TaxVaultFilterState.OFF -> VaultUiPhase.OFF
            else -> VaultUiPhase.ON
        }
    }

    val vaultOpenProgress by animateFloatAsState(
        targetValue = when (uiPhase) {
            VaultUiPhase.OFF -> 0f
            VaultUiPhase.ACTIVATING -> 0.65f
            VaultUiPhase.ON -> 1f
        },
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "vaultOpenProgress"
    )
    val vaultGlowColor by animateColorAsState(
        targetValue = when (uiPhase) {
            VaultUiPhase.ACTIVATING -> Color(0xFFFFC857)
            VaultUiPhase.ON -> GlowGreen
            VaultUiPhase.OFF -> Color(0xFF6B7A94)
        },
        animationSpec = tween(280),
        label = "vaultGlowColor"
    )

    val eligibilityLabel = when (filterState) {
        TaxVaultFilterState.HSA -> EobStrings.t(language, "taxVaultHsaEligibleChip")
        TaxVaultFilterState.FSA -> EobStrings.t(language, "taxVaultFsaEligibleChip")
        TaxVaultFilterState.OFF -> EobStrings.t(language, "taxVaultHsaEligibleChip")
    }

    val controlsEnabled = isGoldTier && uiPhase == VaultUiPhase.ON

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(VaultCardCornerRadius))
            .taxVaultCareTeamBorder(VaultCardCornerRadius)
    ) {
        Column(
            modifier = Modifier
                .background(VaultCardBackground)
                .drawBehind {
                    if (rippleAlpha > 0f) {
                        drawCircle(
                            color = GlowGreen.copy(alpha = rippleAlpha * 0.35f),
                            radius = size.minDimension * (0.4f + rippleAlpha * 0.2f),
                            center = Offset(size.width * 0.22f, size.height * 0.12f)
                        )
                    }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (showTitaniumDoor) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TitaniumVaultBiometricScanner(
                        onVaultUnlocked = {
                            if (!isGoldTier) return@TitaniumVaultBiometricScanner
                            rippleAlpha = 1f
                            uiPhase = VaultUiPhase.ACTIVATING
                            onVaultDoorUnlocked()
                        }
                    )
                    Text(
                        text = EobStrings.t(language, "taxVaultDoorHoldHint"),
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultNeonText.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                VaultDoorGlyph(
                    openProgress = vaultOpenProgress,
                    glowColor = vaultGlowColor,
                    showLockedBadge = !isGoldTier,
                    onClick = {
                        if (!isGoldTier) return@VaultDoorGlyph
                        when (uiPhase) {
                            VaultUiPhase.OFF -> {
                                uiPhase = VaultUiPhase.ACTIVATING
                                rippleAlpha = 1f
                            }
                            else -> onFilterSelected(TaxVaultFilterState.OFF)
                        }
                    }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\$f",
                    color = VaultNeonText,
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                    lineHeight = 34.sp
                )
                Text(
                    text = EobStrings.t(language, "taxVaultFilterTitle"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VaultNeonText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!isGoldTier) {
            Text(
                text = EobStrings.t(language, "taxVaultGoldLocked"),
                style = MaterialTheme.typography.labelSmall,
                color = VaultGoldText,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = EobStrings.t(language, "taxVaultFundTypePrompt"),
            style = MaterialTheme.typography.labelMedium,
            color = VaultNeonText.copy(alpha = 0.88f),
            maxLines = 2
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FundTypePill(
                label = EobStrings.t(language, "taxVaultHsaFunds"),
                selected = filterState == TaxVaultFilterState.HSA,
                enabled = controlsEnabled,
                onClick = { onFilterSelected(TaxVaultFilterState.HSA) },
                modifier = Modifier.weight(1f)
            )
            FundTypePill(
                label = EobStrings.t(language, "taxVaultFsaFunds"),
                selected = filterState == TaxVaultFilterState.FSA,
                enabled = controlsEnabled,
                onClick = { onFilterSelected(TaxVaultFilterState.FSA) },
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = EobStrings.tf(language, "taxVaultShowLabel", eligibilityLabel),
                style = MaterialTheme.typography.labelMedium,
                color = VaultNeonText.copy(alpha = 0.92f),
                maxLines = 2
            )
            Slider(
                value = visibilityMode.ordinal.toFloat(),
                onValueChange = { raw ->
                    if (!controlsEnabled) return@Slider
                    val mode = TaxVaultVisibilityMode.entries.getOrElse(raw.toInt()) {
                        TaxVaultVisibilityMode.GATED
                    }
                    onVisibilityModeSelected(mode)
                },
                valueRange = 0f..2f,
                steps = 1,
                enabled = controlsEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = GlowGreen,
                    inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                TaxVaultVisibilityMode.entries.forEach { mode ->
                    Text(
                        text = EobStrings.t(language, mode.labelKey()),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (mode == visibilityMode && controlsEnabled) {
                            GlowGreen
                        } else {
                            VaultNeonText.copy(alpha = 0.68f)
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VaultKeyGlyph()
            Column(modifier = Modifier.weight(1f)) {
                val statusText = when (uiPhase) {
                    VaultUiPhase.ACTIVATING -> EobStrings.t(language, "taxVaultActivating")
                    VaultUiPhase.OFF -> EobStrings.t(language, "taxVaultOffLabel")
                    VaultUiPhase.ON -> EobStrings.tf(
                        language,
                        "taxVaultStatusFiltering",
                        eligibilityLabel
                    )
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultNeonText.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (uiPhase == VaultUiPhase.ON && budgetSummary.allocationLimit > 0.0) {
                    Text(
                        text = EobStrings.tf(
                            language,
                            "taxVaultBudgetReadout",
                            budgetSummary.eligibleAmount.asCurrency(),
                            budgetSummary.allocationLimit.asCurrency()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = GlowGreen.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        }

        TaxVaultShimmerOverlay(modifier = Modifier.matchParentSize())
    }

    LaunchedEffect(uiPhase) {
        if (uiPhase == VaultUiPhase.ACTIVATING && !showTitaniumDoor) {
            delay(720)
            rippleAlpha = 0f
            onFilterSelected(TaxVaultFilterState.HSA)
            uiPhase = VaultUiPhase.ON
        }
    }
}

@Composable
private fun TaxVaultShimmerOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "taxVaultShimmer")
    val progress by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "taxVaultShimmerProgress"
    )
    Canvas(modifier = modifier) {
        val streakWidth = size.width * 0.48f
        val x = size.width * progress - streakWidth / 2f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    EobBrandGlow.copy(alpha = 0.15f),
                    EobBrandBlue.copy(alpha = 0.45f),
                    EobBrandBlue.copy(alpha = 0.35f),
                    EobBrandBlue.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                start = Offset(x, 0f),
                end = Offset(x + streakWidth, size.height)
            ),
            size = size
        )
    }
}

private fun Modifier.taxVaultCareTeamBorder(cornerRadius: androidx.compose.ui.unit.Dp): Modifier {
    return this
        .drawBehind {
            val shadowPaint = Paint().asFrameworkPaint().apply {
                color = EobBrandGlow.copy(alpha = 0.12f).toArgb()
                setShadowLayer(
                    18.dp.toPx(),
                    0f,
                    0f,
                    EobBrandGlow.copy(alpha = 0.45f).toArgb()
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
        .border(
            width = 2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    EobBrandGlow,
                    EobBrandBlue,
                    EobBrandGlow.copy(alpha = 0.85f),
                    EobBrandBlue
                )
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
}

@Composable
private fun FundTypePill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) Color.White else Color.White.copy(alpha = 0.14f)
    val textColor = if (selected) Color(0xFF0B1F45) else VaultNeonText.copy(alpha = 0.88f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .border(1.dp, textColor.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VaultDoorGlyph(
    openProgress: Float,
    glowColor: Color,
    showLockedBadge: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.28f + openProgress * 0.4f),
                        Color(0xFF1E3358)
                    )
                )
            )
            .border(1.dp, glowColor.copy(alpha = 0.45f + openProgress * 0.4f), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CanvasVaultDoorIcon(
            openProgress = openProgress,
            glowColor = glowColor,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        )
        if (showLockedBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VaultGoldText),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔒", fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun CanvasVaultDoorIcon(
    openProgress: Float,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val frameColor = Color.White.copy(alpha = 0.85f)
        val doorWidth = size.width * 0.42f
        val doorHeight = size.height * 0.72f
        val left = (size.width - doorWidth) / 2f
        val top = size.height * 0.14f
        drawRoundRect(
            color = frameColor.copy(alpha = 0.35f),
            topLeft = Offset(left - 4f, top - 4f),
            size = androidx.compose.ui.geometry.Size(doorWidth + 8f, doorHeight + 8f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            style = Stroke(width = 2f)
        )
        val split = doorWidth * (0.35f + openProgress * 0.3f)
        drawRoundRect(
            color = glowColor.copy(alpha = 0.75f),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(split, doorHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = glowColor.copy(alpha = 0.55f),
            topLeft = Offset(left + split + 2f, top),
            size = androidx.compose.ui.geometry.Size(doorWidth - split - 2f, doorHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )
        if (openProgress > 0.85f) {
            drawCircle(
                color = GlowGreen,
                radius = 5f,
                center = Offset(left + doorWidth * 0.72f, top + doorHeight * 0.5f)
            )
        }
    }
}

@Composable
private fun VaultKeyGlyph() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-5).dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(width = 13.dp, height = 17.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(2.dp))
            )
        }
        Text(
            text = "🔑",
            modifier = Modifier.padding(start = 2.dp),
            fontSize = 15.sp
        )
    }
}
