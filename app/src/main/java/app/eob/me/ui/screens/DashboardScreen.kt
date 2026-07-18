package app.eob.me.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.asCurrency
import app.eob.me.ui.components.HolographicGlassCard
import app.eob.me.ui.theme.EobAdjustmentGreen
import app.eob.me.ui.theme.EobBilledBlue
import app.eob.me.ui.theme.EobPatientRed

private val BilledBlue = EobBilledBlue
private val AdjustmentGreen = EobAdjustmentGreen
private val PatientRed = EobPatientRed

@Composable
fun DashboardScreen(
    language: AppLanguage,
    records: List<EobRecord>,
    modifier: Modifier = Modifier
) {
    val metrics = remember(records) {
        val grossBilled = records.sumOf { it.totalBilledAmount }
        val adjustments = records.sumOf { it.totalContractualAdjustmentAmount }
        val patientDue = records.sumOf { it.totalPatientResponsibility }
        val insurancePaid = (grossBilled - adjustments - patientDue).coerceAtLeast(0.0)

        FinancialMetrics(
            grossBilled = grossBilled,
            adjustments = adjustments,
            insurancePaid = insurancePaid,
            patientDue = patientDue
        )
    }

    val providerBreakdown = remember(records, language) {
        records.groupBy { it.providerName }
            .map { (provider, recordList) ->
                ProviderCostRow(
                    name = provider.ifBlank { EobStrings.t(language, "unknownProvider") },
                    totalAmount = recordList.sumOf { it.totalBilledAmount },
                    patientAmount = recordList.sumOf { it.totalPatientResponsibility }
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    val allocationSlices = remember(metrics, language) {
        listOf(
            AllocationSlice(
                label = EobStrings.t(language, "networkSavingsAdjustments"),
                amount = metrics.adjustments,
                color = AdjustmentGreen
            ),
            AllocationSlice(
                label = EobStrings.t(language, "coveredByCarrierPlan"),
                amount = metrics.insurancePaid,
                color = BilledBlue
            ),
            AllocationSlice(
                label = EobStrings.t(language, "yourPatientResponsibility"),
                amount = metrics.patientDue,
                color = PatientRed
            )
        ).filter { it.amount > 0.0 }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
    ) {
        item {
            Text(
                text = EobStrings.t(language, "expenseAnalytics"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (records.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = EobStrings.t(language, "dashboardUploadHint"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                HolographicGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Text(
                        text = EobStrings.t(language, "claimAllocationProfile"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = EobStrings.tf(language, "totalAggregatedClaims", metrics.grossBilled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ClaimAllocationPieChart(
                        slices = allocationSlices,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    allocationSlices.forEach { slice ->
                        LegendItem(
                            label = slice.label,
                            amount = slice.amount,
                            color = slice.color
                        )
                    }
                }
            }

            item {
                HolographicGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Text(
                        text = EobStrings.t(language, "spendingByFacility"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FacilitySpendingBarChart(
                        language = language,
                        providers = providerBreakdown
                    )
                }
            }
        }
    }
}

@Composable
private fun ClaimAllocationPieChart(
    slices: List<AllocationSlice>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (slices.isEmpty()) {
            Text(
                text = "—",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        Canvas(modifier = Modifier.size(180.dp)) {
            val diameter = size.minDimension
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val total = slices.sumOf { it.amount }
            var startAngle = -90f

            slices.forEach { slice ->
                val sweepAngle = ((slice.amount / total) * 360f).toFloat()
                if (sweepAngle > 0f) {
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize
                    )
                    startAngle += sweepAngle
                }
            }
        }
    }
}

@Composable
private fun FacilitySpendingBarChart(
    language: AppLanguage,
    providers: List<ProviderCostRow>,
    modifier: Modifier = Modifier
) {
    if (providers.isEmpty()) {
        Text(
            text = EobStrings.t(language, "dashboardUploadHint"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val maxAmount = providers.maxOf { it.totalAmount }.coerceAtLeast(1.0)
    val facilityTotal = providers.sumOf { it.totalAmount }
    val patientTotal = providers.sumOf { it.patientAmount }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        providers.forEach { summary ->
            FacilityBarRow(
                name = summary.name,
                amount = summary.totalAmount,
                patientAmount = summary.patientAmount,
                maxAmount = maxAmount,
                language = language
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = EobStrings.t(language, "facilitySpendingTotal"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = facilityTotal.asCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BilledBlue
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = EobStrings.t(language, "facilityPatientTotal"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = patientTotal.asCurrency(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (patientTotal > 0.0) PatientRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FacilityBarRow(
    name: String,
    amount: Double,
    patientAmount: Double,
    maxAmount: Double,
    language: AppLanguage
) {
    val barFraction = (amount / maxAmount).toFloat().coerceIn(0.05f, 1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Text(
                text = amount.asCurrency(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.End
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BilledBlue)
            )
        }

        Text(
            text = EobStrings.tf(language, "patientOutOfPocketShare", patientAmount),
            style = MaterialTheme.typography.bodySmall,
            color = if (patientAmount > 0) PatientRed else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendItem(label: String, amount: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = amount.asCurrency(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class FinancialMetrics(
    val grossBilled: Double,
    val adjustments: Double,
    val insurancePaid: Double,
    val patientDue: Double
)

private data class AllocationSlice(
    val label: String,
    val amount: Double,
    val color: Color
)

private data class ProviderCostRow(
    val name: String,
    val totalAmount: Double,
    val patientAmount: Double
)

internal data class ProviderNameLines(
    val firstLine: String,
    val secondLine: String?
)

/**
 * When a provider name is long enough to crowd the billed amount on phone layouts,
 * move the last word to a second line so currency stays horizontal.
 */
internal fun splitProviderNameForDashboardRow(
    providerName: String,
    maxSingleLineChars: Int = 28
): ProviderNameLines {
    val trimmed = providerName.trim()
    if (trimmed.length <= maxSingleLineChars) {
        return ProviderNameLines(firstLine = trimmed, secondLine = null)
    }
    val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size < 2) {
        return ProviderNameLines(firstLine = trimmed, secondLine = null)
    }
    return ProviderNameLines(
        firstLine = words.dropLast(1).joinToString(" "),
        secondLine = words.last()
    )
}
