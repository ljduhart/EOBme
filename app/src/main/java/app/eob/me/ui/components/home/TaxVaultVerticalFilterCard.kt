package app.eob.me.ui.components.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import app.eob.me.ui.theme.EobBrandCyan
import kotlinx.coroutines.delay

private enum class VaultUiPhase {
    OFF,
    ACTIVATING,
    ON
}

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

    val cardShape = RoundedCornerShape(22.dp)
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B1F45),
            Color(0xFF12356B),
            Color(0xFF0A224F)
        )
    )
    val glowGreen = Color(0xFF3DDC84)
    val lockedAlpha = if (isGoldTier) 1f else 0.55f
    val vaultOpenProgress by animateFloatAsState(
        targetValue = when (uiPhase) {
            VaultUiPhase.OFF -> 0f
            VaultUiPhase.ACTIVATING -> 0.65f
            VaultUiPhase.ON -> 1f
        },
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "vaultOpenProgress"
    )
    val toggleColor by animateColorAsState(
        targetValue = when {
            uiPhase == VaultUiPhase.ACTIVATING -> Color(0xFFFFC857)
            uiPhase == VaultUiPhase.ON -> glowGreen
            else -> Color(0xFF8A93A8)
        },
        animationSpec = tween(280),
        label = "vaultToggleColor"
    )

    val eligibilityLabel = when (filterState) {
        TaxVaultFilterState.HSA -> EobStrings.t(language, "taxVaultHsaEligibleChip")
        TaxVaultFilterState.FSA -> EobStrings.t(language, "taxVaultFsaEligibleChip")
        TaxVaultFilterState.OFF -> EobStrings.t(language, "taxVaultHsaEligibleChip")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(lockedAlpha)
            .clip(cardShape)
            .background(gradient)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        EobBrandCyan.copy(alpha = 0.55f),
                        EobBrandBlue.copy(alpha = 0.25f),
                        EobBrandCyan.copy(alpha = 0.4f)
                    )
                ),
                shape = cardShape
            )
            .drawBehind {
                if (rippleAlpha > 0f) {
                    drawCircle(
                        color = glowGreen.copy(alpha = rippleAlpha * 0.35f),
                        radius = size.minDimension * (0.35f + rippleAlpha * 0.25f),
                        center = Offset(size.width * 0.82f, size.height * 0.18f)
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VaultDoorGlyph(
                openProgress = vaultOpenProgress,
                isActive = uiPhase != VaultUiPhase.OFF,
                glowColor = glowGreen
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\$f",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
                Text(
                    text = EobStrings.t(language, "taxVaultFilterTitle"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = EobStrings.t(language, "taxVaultHeaderSubtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
            VaultMasterToggle(
                language = language,
                phase = uiPhase,
                toggleColor = toggleColor,
                enabled = isGoldTier,
                onToggle = {
                    if (!isGoldTier) return@VaultMasterToggle
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

        if (!isGoldTier) {
            Text(
                text = EobStrings.t(language, "taxVaultGoldLocked"),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFD166),
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = EobStrings.t(language, "taxVaultFundTypePrompt"),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FundTypePill(
                label = EobStrings.t(language, "taxVaultHsaFunds"),
                selected = filterState == TaxVaultFilterState.HSA,
                enabled = isGoldTier && uiPhase == VaultUiPhase.ON,
                onClick = { onFilterSelected(TaxVaultFilterState.HSA) },
                modifier = Modifier.weight(1f)
            )
            FundTypePill(
                label = EobStrings.t(language, "taxVaultFsaFunds"),
                selected = filterState == TaxVaultFilterState.FSA,
                enabled = isGoldTier && uiPhase == VaultUiPhase.ON,
                onClick = { onFilterSelected(TaxVaultFilterState.FSA) },
                modifier = Modifier.weight(1f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = EobStrings.tf(language, "taxVaultShowLabel", eligibilityLabel),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            val sliderValue = visibilityMode.ordinal.toFloat()
            Slider(
                value = sliderValue,
                onValueChange = { raw ->
                    if (!isGoldTier || uiPhase != VaultUiPhase.ON) return@Slider
                    val mode = TaxVaultVisibilityMode.entries.getOrElse(raw.toInt()) {
                        TaxVaultVisibilityMode.GATED
                    }
                    onVisibilityModeSelected(mode)
                },
                valueRange = 0f..2f,
                steps = 1,
                enabled = isGoldTier && uiPhase == VaultUiPhase.ON,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = glowGreen,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                TaxVaultVisibilityMode.entries.forEach { mode ->
                    Text(
                        text = EobStrings.t(language, mode.labelKey()),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mode == visibilityMode) glowGreen else Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

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
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
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
                        color = glowGreen.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }

    LaunchedEffect(uiPhase) {
        if (uiPhase == VaultUiPhase.ACTIVATING) {
            delay(720)
            rippleAlpha = 0f
            onFilterSelected(TaxVaultFilterState.HSA)
            uiPhase = VaultUiPhase.ON
        }
    }
}

@Composable
private fun VaultMasterToggle(
    language: AppLanguage,
    phase: VaultUiPhase,
    toggleColor: Color,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 30.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A2B4C))
                .border(1.dp, toggleColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = if (enabled) ripple(bounded = true) else null,
                    enabled = enabled,
                    onClick = onToggle
                ),
            contentAlignment = if (phase == VaultUiPhase.OFF) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(toggleColor)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when (phase) {
                VaultUiPhase.ACTIVATING -> EobStrings.t(language, "taxVaultActivating")
                VaultUiPhase.ON -> EobStrings.t(language, "taxVaultOnLabel")
                VaultUiPhase.OFF -> EobStrings.t(language, "taxVaultOffLabel")
            },
            style = MaterialTheme.typography.labelSmall,
            color = toggleColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun FundTypePill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) Color.White else Color.White.copy(alpha = 0.12f)
    val textColor = if (selected) Color(0xFF0B1F45) else Color.White.copy(alpha = if (enabled) 0.9f else 0.45f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .border(1.dp, textColor.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VaultDoorGlyph(
    openProgress: Float,
    isActive: Boolean,
    glowColor: Color
) {
    val doorColor = if (isActive) glowColor.copy(alpha = 0.25f + openProgress * 0.45f) else Color(0xFF4A5D7A)
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(doorColor, Color(0xFF1E3358))
                )
            )
            .border(1.dp, glowColor.copy(alpha = 0.35f + openProgress * 0.5f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (openProgress > 0.85f) "✓" else "🔒",
            fontSize = if (openProgress > 0.85f) 20.sp else 18.sp,
            color = if (openProgress > 0.85f) glowColor else Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun VaultKeyGlyph() {
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            )
        }
        Text(
            text = "🔑",
            modifier = Modifier.padding(start = 2.dp),
            fontSize = 16.sp
        )
    }
}
