package app.eob.me.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.YtdExpenseData
import app.eob.me.data.YtdExpenseYearSelection
import app.eob.me.data.YtdMetricKind
import app.eob.me.data.YtdMetricSection
import app.eob.me.data.asCurrency
import kotlin.math.roundToInt

private val GaugeTeal = Color(0xFF00695C)
private val GaugeGold = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtdExpenseScreen(
    language: AppLanguage,
    data: YtdExpenseData,
    yearOptions: List<YtdExpenseYearSelection>,
    selectedYear: YtdExpenseYearSelection,
    onYearSelected: (YtdExpenseYearSelection) -> Unit,
    yearOptionLabel: (YtdExpenseYearSelection) -> String,
    modifier: Modifier = Modifier
) {
    var animateEntrance by remember { mutableStateOf(false) }

    LaunchedEffect(data.year, data.aggregatesAllYears, data.eobCount) {
        animateEntrance = false
        animateEntrance = true
    }

    val deductibleSweep by animateFloatAsState(
        targetValue = if (animateEntrance) data.deductibleProgress else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "deductibleSweep"
    )
    val outOfPocketSweep by animateFloatAsState(
        targetValue = if (animateEntrance) data.outOfPocketProgress else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "outOfPocketSweep"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = EobStrings.t(language, "ytdHealthGoalsTitle"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        YtdSummaryHeaderCard(
            language = language,
            data = data,
            yearOptions = yearOptions,
            selectedYear = selectedYear,
            onYearSelected = onYearSelected,
            yearOptionLabel = yearOptionLabel
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressGaugeCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                title = EobStrings.t(language, "ytdDeductibleMet"),
                sweepFraction = deductibleSweep,
                currentAmount = data.deductibles,
                maxAmount = data.deductibleMax,
                towardsGoalLabel = EobStrings.t(language, "ytdTowardsGoal"),
                paidOfGoalFormatter = { current, max ->
                    EobStrings.tf(language, "ytdPaidOfGoal", current, max)
                }
            )
            ProgressGaugeCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                title = EobStrings.t(language, "ytdOopMaxProgress"),
                sweepFraction = outOfPocketSweep,
                currentAmount = data.patientResponsibility,
                maxAmount = data.outOfPocketMax,
                towardsGoalLabel = EobStrings.t(language, "ytdTowardsGoal"),
                paidOfGoalFormatter = { current, max ->
                    EobStrings.tf(language, "ytdPaidOfGoal", current, max)
                }
            )
        }

        YtdExpandableMetricSections(language = language, data = data)

        Spacer(modifier = Modifier.height(76.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YtdSummaryHeaderCard(
    language: AppLanguage,
    data: YtdExpenseData,
    yearOptions: List<YtdExpenseYearSelection>,
    selectedYear: YtdExpenseYearSelection,
    onYearSelected: (YtdExpenseYearSelection) -> Unit,
    yearOptionLabel: (YtdExpenseYearSelection) -> String
) {
    var yearMenuExpanded by remember { mutableStateOf(false) }
    val summaryTitle = EobStrings.t(language, "ytdYearlyExpenseSummary")
    val titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    val density = LocalDensity.current
    var yearToggleWidth by remember(summaryTitle) { mutableStateOf(0.dp) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = summaryTitle,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onTextLayout = { layout ->
                    yearToggleWidth = ytdYearToggleWidthUnderSummary(
                        title = summaryTitle,
                        layout = layout,
                        density = density
                    )
                }
            )
            ExposedDropdownMenuBox(
                expanded = yearMenuExpanded,
                onExpandedChange = { yearMenuExpanded = it },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .then(
                        if (yearToggleWidth > 0.dp) {
                            Modifier.width(yearToggleWidth)
                        } else {
                            Modifier.wrapContentWidth(Alignment.Start)
                        }
                    )
            ) {
                OutlinedTextField(
                    value = yearOptionLabel(selectedYear),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                DropdownMenu(
                    expanded = yearMenuExpanded,
                    onDismissRequest = { yearMenuExpanded = false }
                ) {
                    yearOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(yearOptionLabel(option)) },
                            onClick = {
                                onYearSelected(option)
                                yearMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Text(
                text = when {
                    data.eobCount == 0 -> EobStrings.t(language, "yearlyNoEobs")
                    data.aggregatesAllYears ->
                        EobStrings.tf(language, "ytdAllYearsEobsSubtitle", data.eobCount)
                    else -> EobStrings.tf(language, "ytdYearEobsSubtitle", data.year, data.eobCount)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ProgressGaugeCard(
    title: String,
    sweepFraction: Float,
    currentAmount: Double,
    maxAmount: Double,
    towardsGoalLabel: String,
    paidOfGoalFormatter: (String, String) -> String,
    modifier: Modifier = Modifier
) {
    val percentLabel = "${(sweepFraction * 100f).roundToInt()}%"
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.size(128.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 30f
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    val sweepAngle = (sweepFraction.coerceIn(0f, 1f) * 360f)
                    if (sweepAngle > 0f) {
                        drawArc(
                            brush = Brush.linearGradient(
                                colors = listOf(GaugeTeal, GaugeGold),
                                start = topLeft,
                                end = Offset(topLeft.x + arcSize.width, topLeft.y + arcSize.height)
                            ),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = percentLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = towardsGoalLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentAmount.asCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(top = 2.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = paidOfGoalFormatter(currentAmount.asCurrency(), maxAmount.asCurrency()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun YtdExpandableMetricSections(language: AppLanguage, data: YtdExpenseData) {
    var expandedKind by remember { mutableStateOf<YtdMetricKind?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        data.metricSections.forEach { section ->
            val presentation = ytdMetricPresentation(language, section.kind)
            ExpandableYtdMetricCard(
                language = language,
                section = section,
                label = presentation.label,
                icon = presentation.icon,
                iconBackground = presentation.iconBackground,
                iconTint = presentation.iconTint,
                expanded = expandedKind == section.kind,
                onToggle = {
                    expandedKind = if (expandedKind == section.kind) null else section.kind
                }
            )
        }
    }
}

private data class YtdMetricPresentation(
    val label: String,
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color
)

private fun ytdMetricPresentation(language: AppLanguage, kind: YtdMetricKind): YtdMetricPresentation {
    return when (kind) {
        YtdMetricKind.Copay -> YtdMetricPresentation(
            label = EobStrings.t(language, "ytdCopaysPaidYtd"),
            icon = Icons.Rounded.CreditCard,
            iconBackground = Color(0xFFE3F2FD),
            iconTint = Color(0xFF1565C0)
        )
        YtdMetricKind.Coinsurance -> YtdMetricPresentation(
            label = EobStrings.t(language, "ytdCoinsurancePaidYtd"),
            icon = Icons.Rounded.Percent,
            iconBackground = Color(0xFFF3E5F5),
            iconTint = Color(0xFF7B1FA2)
        )
        YtdMetricKind.TotalBilled -> YtdMetricPresentation(
            label = EobStrings.t(language, "totalBilled"),
            icon = Icons.Rounded.LocalHospital,
            iconBackground = Color(0xFFFFEBEE),
            iconTint = Color(0xFFC62828)
        )
        YtdMetricKind.InsurancePaid -> YtdMetricPresentation(
            label = EobStrings.t(language, "ytdInsurancePaidYtd"),
            icon = Icons.Rounded.Shield,
            iconBackground = Color(0xFFE8F5E9),
            iconTint = Color(0xFF2E7D32)
        )
        YtdMetricKind.Adjustments -> YtdMetricPresentation(
            label = EobStrings.t(language, "ytdAdjustmentsYtd"),
            icon = Icons.Rounded.AccountBalance,
            iconBackground = Color(0xFFFFF8E1),
            iconTint = Color(0xFFF57F17)
        )
        YtdMetricKind.Deductible -> YtdMetricPresentation(
            label = EobStrings.t(language, "ytdDeductiblesPaidYtd"),
            icon = Icons.Rounded.Savings,
            iconBackground = Color(0xFFE0F2F1),
            iconTint = Color(0xFF00695C)
        )
    }
}

@Composable
private fun ExpandableYtdMetricCard(
    language: AppLanguage,
    section: YtdMetricSection,
    label: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBackground, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = section.total.asCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (section.lineItems.isEmpty()) {
                        Text(
                            text = EobStrings.t(language, "ytdMetricNoLineItems"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        section.lineItems.forEach { lineItem ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lineItem.serviceDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = lineItem.amount.asCurrency(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = EobStrings.t(language, "ytdMetricTotal"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = section.total.asCurrency(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricDetailCard(
    label: String,
    amount: Double,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBackground, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount.asCurrency(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

internal fun ytdSummaryTitleAnchorEndIndex(title: String): Int {
    val summaryMatch = Regex("Summary", RegexOption.IGNORE_CASE).find(title)
    if (summaryMatch != null) {
        return summaryMatch.range.last + 1
    }
    return title.length
}

internal fun ytdYearToggleWidthUnderSummary(
    title: String,
    layout: TextLayoutResult,
    density: Density
): Dp {
    if (title.isBlank()) return 0.dp
    val anchorEndIndex = ytdSummaryTitleAnchorEndIndex(title).coerceIn(1, title.length)
    val anchorRightPx = layout.getBoundingBox(anchorEndIndex - 1).right
    return with(density) {
        anchorRightPx.toDp().coerceAtLeast(1.dp)
    }
}
