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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import app.eob.me.data.VaultEvidenceThumbnail
import app.eob.me.data.asCurrency
import app.eob.me.ui.components.taxvault.VaultEvidenceCarousel
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandGlow
import app.eob.me.ui.theme.EobInsuranceGradientEnd
import app.eob.me.ui.theme.EobInsuranceGradientMid
import app.eob.me.ui.theme.EobInsuranceGradientStart
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class VaultUiPhase {
    OFF,
    ACTIVATING,
    ON
}

private val VaultCardCornerRadius = 16.dp
private val VaultCardInnerPaddingHorizontal = 16.dp
private val VaultCardInnerPaddingVertical = 14.dp
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
private val VaultPrimaryText = Color.White
private val VaultNeonText = EobBrandBlue
private val VaultSilver = Color(0xFFB8C4D0)
private val BinaryBeamColor = Color(0xFF7AD7FF)

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
    enableShimmerOverlay: Boolean = true,
    showMiniatureEvidence: Boolean = false,
    evidenceThumbnails: List<VaultEvidenceThumbnail> = emptyList(),
    onEvidenceSelected: (String) -> Unit = {},
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
        Box(
            modifier = Modifier
                .background(VaultCardBackground)
                .drawBehind {
                    drawTaxVaultCentralLightRay()
                    if (rippleAlpha > 0f) {
                        drawCircle(
                            color = GlowGreen.copy(alpha = rippleAlpha * 0.35f),
                            radius = size.minDimension * (0.4f + rippleAlpha * 0.2f),
                            center = Offset(size.width * 0.16f, size.height * 0.1f)
                        )
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = VaultCardInnerPaddingHorizontal,
                        vertical = VaultCardInnerPaddingVertical
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TaxVaultBrandingHeader(
                    language = language,
                    openProgress = vaultOpenProgress,
                    glowColor = vaultGlowColor,
                    showLockedBadge = !isGoldTier,
                    showTitaniumDoor = showTitaniumDoor,
                    onVaultDoorClick = {
                        if (showTitaniumDoor || !isGoldTier) return@TaxVaultBrandingHeader
                        when (uiPhase) {
                            VaultUiPhase.OFF -> {
                                uiPhase = VaultUiPhase.ACTIVATING
                                rippleAlpha = 1f
                            }
                            else -> onFilterSelected(TaxVaultFilterState.OFF)
                        }
                    },
                    onVaultUnlocked = {
                        if (!isGoldTier) return@TaxVaultBrandingHeader
                        rippleAlpha = 1f
                        uiPhase = VaultUiPhase.ACTIVATING
                        onVaultDoorUnlocked()
                    }
                )

            if (!isGoldTier) {
                Text(
                    text = EobStrings.t(language, "taxVaultGoldLocked"),
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultGoldText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = EobStrings.t(language, "taxVaultFundTypePrompt"),
                style = MaterialTheme.typography.labelMedium,
                color = VaultPrimaryText.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
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

            Text(
                text = EobStrings.tf(language, "taxVaultShowLabel", eligibilityLabel),
                style = MaterialTheme.typography.labelMedium,
                color = VaultPrimaryText.copy(alpha = 0.92f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                        activeTrackColor = EobBrandBlue,
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
                                VaultNeonText
                            } else {
                                VaultPrimaryText.copy(alpha = 0.68f)
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (showMiniatureEvidence && evidenceThumbnails.isNotEmpty()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val miniatureCardWidth = (maxWidth / 3.2f).coerceIn(88.dp, 108.dp)
                    VaultEvidenceCarousel(
                        language = language,
                        thumbnails = evidenceThumbnails,
                        onEvidenceSelected = onEvidenceSelected,
                        titleColor = VaultPrimaryText,
                        miniatureCardWidth = miniatureCardWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VaultKeyCardsAndBinaryGlyph(
                    modifier = Modifier.width(88.dp)
                )
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
                        color = VaultPrimaryText.copy(alpha = 0.92f),
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
        }

        if (enableShimmerOverlay) {
            TaxVaultShimmerOverlay(modifier = Modifier.matchParentSize())
        }
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
private fun TaxVaultBrandingHeader(
    language: AppLanguage,
    openProgress: Float,
    glowColor: Color,
    showLockedBadge: Boolean,
    showTitaniumDoor: Boolean,
    onVaultDoorClick: () -> Unit,
    onVaultUnlocked: () -> Unit
) {
    val displayOpenProgress = openProgress.coerceAtLeast(0.42f)
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (showTitaniumDoor) 58.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TaxVaultVaultIconCluster(
                    openProgress = displayOpenProgress,
                    glowColor = EobBrandBlue,
                    showLockedBadge = showLockedBadge,
                    onClick = onVaultDoorClick,
                    modifier = Modifier.size(68.dp)
                )
                Text(
                    text = "\$f",
                    color = VaultPrimaryText,
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = EobStrings.t(language, "taxVaultFilterTitle"),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 24.sp
                ),
                fontWeight = FontWeight.Bold,
                color = VaultPrimaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showTitaniumDoor) {
            Column(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TitaniumVaultBiometricScanner(
                    onVaultUnlocked = onVaultUnlocked,
                    scannerSize = 48.dp
                )
                Text(
                    text = EobStrings.t(language, "taxVaultDoorHoldHint"),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp, lineHeight = 7.sp),
                    color = VaultPrimaryText.copy(alpha = 0.62f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(64.dp)
                )
            }
        }
    }
}

@Composable
private fun TaxVaultVaultIconCluster(
    openProgress: Float,
    glowColor: Color,
    showLockedBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbitRadius = size.minDimension * 0.44f
            drawCircle(
                color = EobBrandGlow.copy(alpha = 0.16f),
                radius = orbitRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )
            listOf(0f, 90f, 180f, 270f).forEach { degrees ->
                rotate(degrees, center) {
                    drawArc(
                        color = EobBrandBlue.copy(alpha = 0.55f),
                        startAngle = -28f,
                        sweepAngle = 42f,
                        useCenter = false,
                        topLeft = Offset(center.x - orbitRadius, center.y - orbitRadius),
                        size = Size(orbitRadius * 2f, orbitRadius * 2f),
                        style = Stroke(width = 2.8f)
                    )
                }
            }
            repeat(3) { index ->
                val gearCenter = Offset(
                    x = size.width * (0.18f + index * 0.28f),
                    y = size.height * 0.2f
                )
                drawGear(gearCenter, radius = 5.5f + index, teeth = 6, color = VaultSilver.copy(alpha = 0.35f))
            }
        }
        VaultDoorGlyph(
            openProgress = openProgress,
            glowColor = glowColor,
            showLockedBadge = showLockedBadge,
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun VaultKeyCardsAndBinaryGlyph(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "vaultCardsBinary")
    val scroll by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vaultCardsBinaryScroll"
    )
    val binaryColumns = remember {
        listOf("10101", "01100", "11010", "00010")
    }
    Box(
        modifier = modifier.height(72.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cardWidth = size.width * 0.22f
            val cardHeight = size.height * 0.34f
            val baseY = size.height * 0.58f
            repeat(3) { index ->
                val offsetX = index * 9f
                val offsetY = index * 4f
                drawRoundRect(
                    color = EobBrandBlue.copy(alpha = 0.35f),
                    topLeft = Offset(4f + offsetX, baseY - offsetY),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = Stroke(width = 1.6f)
                )
            }
            val keyY = baseY - cardHeight * 0.35f
            drawRoundRect(
                color = VaultPrimaryText.copy(alpha = 0.92f),
                topLeft = Offset(8f, keyY),
                size = Size(size.width * 0.52f, 5f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawCircle(
                color = VaultPrimaryText.copy(alpha = 0.92f),
                radius = 6f,
                center = Offset(size.width * 0.62f, keyY + 2.5f)
            )
            val paint = Paint().asFrameworkPaint().apply {
                color = BinaryBeamColor.copy(alpha = 0.78f).toArgb()
                textSize = 7.dp.toPx()
                isAntiAlias = true
            }
            binaryColumns.forEachIndexed { columnIndex, column ->
                val x = 6f + columnIndex * 10f
                column.forEachIndexed { rowIndex, digit ->
                    val y = baseY - ((rowIndex + scroll * 5f) % 6f) * 9f - 8f
                    if (y > 0f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            digit.toString(),
                            x,
                            y,
                            paint
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGear(center: Offset, radius: Float, teeth: Int, color: Color) {
    drawCircle(color = color.copy(alpha = 0.5f), radius = radius * 0.55f, center = center)
    repeat(teeth) { index ->
        val angle = (index.toFloat() / teeth) * (2f * PI.toFloat())
        val outer = Offset(
            x = center.x + cos(angle) * radius,
            y = center.y + sin(angle) * radius
        )
        drawCircle(color = color, radius = radius * 0.22f, center = outer)
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

private fun DrawScope.drawTaxVaultCentralLightRay() {
    val rayTop = Offset(size.width * 0.14f, size.height * 0.04f)
    val rayBottom = Offset(size.width * 0.12f, size.height * 0.94f)
    val topHalfWidth = size.width * 0.2f
    val bottomHalfWidth = size.width * 0.07f

    val beamPath = Path().apply {
        moveTo(rayTop.x - topHalfWidth, rayTop.y)
        lineTo(rayTop.x + topHalfWidth, rayTop.y)
        lineTo(rayBottom.x + bottomHalfWidth, rayBottom.y)
        lineTo(rayBottom.x - bottomHalfWidth, rayBottom.y)
        close()
    }
    drawPath(
        path = beamPath,
        brush = Brush.linearGradient(
            colors = listOf(
                EobBrandGlow.copy(alpha = 0.62f),
                EobBrandBlue.copy(alpha = 0.42f),
                EobBrandBlue.copy(alpha = 0.22f),
                EobBrandBlue.copy(alpha = 0.06f)
            ),
            start = rayTop,
            end = rayBottom
        )
    )
    drawPath(
        path = beamPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                EobBrandGlow.copy(alpha = 0.12f),
                Color.Transparent
            ),
            start = rayTop,
            end = Offset(rayBottom.x, size.height * 0.55f)
        )
    )
    drawCircle(
        color = EobBrandGlow.copy(alpha = 0.35f),
        radius = topHalfWidth * 0.55f,
        center = rayTop
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
    val textColor = if (selected) Color(0xFF0B1F45) else VaultPrimaryText.copy(alpha = 0.92f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(12.dp)
                .border(1.dp, textColor.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun VaultDoorGlyph(
    openProgress: Float,
    glowColor: Color,
    showLockedBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
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
    Canvas(modifier = modifier) {
        val frameColor = Color.White.copy(alpha = 0.85f)
        val doorWidth = size.width * 0.42f
        val doorHeight = size.height * 0.72f
        val left = (size.width - doorWidth) / 2f
        val top = size.height * 0.14f
        drawRoundRect(
            color = frameColor.copy(alpha = 0.35f),
            topLeft = Offset(left - 4f, top - 4f),
            size = Size(doorWidth + 8f, doorHeight + 8f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f)
        )
        val split = doorWidth * (0.35f + openProgress * 0.3f)
        drawRoundRect(
            color = glowColor.copy(alpha = 0.75f),
            topLeft = Offset(left, top),
            size = Size(split, doorHeight),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = glowColor.copy(alpha = 0.55f),
            topLeft = Offset(left + split + 2f, top),
            size = Size(doorWidth - split - 2f, doorHeight),
            cornerRadius = CornerRadius(3f, 3f)
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
