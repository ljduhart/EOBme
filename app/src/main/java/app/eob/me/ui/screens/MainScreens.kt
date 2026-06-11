package app.eob.me.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import app.eob.me.ui.theme.EobChartBlue
import app.eob.me.ui.theme.EobChartGreen
import app.eob.me.ui.theme.EobChartIndigo
import app.eob.me.ui.theme.EobChartOrange
import app.eob.me.ui.theme.EobChartPurple
import app.eob.me.ui.theme.EobChartRed
import app.eob.me.ui.theme.EobChartTeal
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.YearlyHealthCostSummary
import app.eob.me.data.asCurrency

@Composable
fun YearlyHealthCostDashboard(language: AppLanguage, summary: YearlyHealthCostSummary) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                EobStrings.t(language, "yearlyHealthCostDashboard"),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                EobStrings.tf(
                    language,
                    "yearLabel",
                    if (summary.year == 0) {
                        EobStrings.t(language, "yearlyNoEobs")
                    } else {
                        summary.year.toString()
                    },
                    summary.eobCount
                )
            )
            AmountRow(EobStrings.t(language, "totalBilled"), summary.totalBilled)
            AmountRow(EobStrings.t(language, "totalInsurancePaid"), summary.totalInsurancePaid)
            AmountRow(EobStrings.t(language, "contractualAdjustments"), summary.totalContractualAdjustment)
            AmountRow(EobStrings.t(language, "patientResponsibility"), summary.totalPatientResponsibility)
            AmountRow(EobStrings.t(language, "copays"), summary.totalCopay)
            AmountRow(EobStrings.t(language, "deductibles"), summary.totalDeductible)
            AmountRow(EobStrings.t(language, "coinsuranceLabel"), summary.totalCoinsurance)
        }
    }
}

@Composable
internal fun AmountRow(label: String, amount: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(amount.asCurrency())
    }
}

@Composable
fun YearlyHealthCostBarChart(language: AppLanguage, summary: YearlyHealthCostSummary) {
    val segments = remember(summary, language) {
        listOf(
            EobStrings.t(language, "totalBilled") to summary.totalBilled,
            EobStrings.t(language, "totalInsurancePaid") to summary.totalInsurancePaid,
            EobStrings.t(language, "contractualAdjustments") to summary.totalContractualAdjustment,
            EobStrings.t(language, "patientResponsibility") to summary.totalPatientResponsibility,
            EobStrings.t(language, "copays") to summary.totalCopay,
            EobStrings.t(language, "deductibles") to summary.totalDeductible,
            EobStrings.t(language, "coinsuranceLabel") to summary.totalCoinsurance
        )
    }
    val maxValue = segments.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val barColors = listOf(
        EobChartBlue,
        EobChartGreen,
        EobChartPurple,
        EobChartRed,
        EobChartOrange,
        EobChartTeal,
        EobChartIndigo
    )

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                EobStrings.t(language, "yearlyExpenseChartTitle"),
                style = MaterialTheme.typography.titleMedium
            )
            if (summary.eobCount == 0) {
                Text(
                    EobStrings.t(language, "yearlyExpenseChartEmpty"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val barCount = segments.size
                    val gap = size.width * 0.02f
                    val barWidth = (size.width - gap * (barCount + 1)) / barCount
                    segments.forEachIndexed { index, (_, amount) ->
                        val fraction = (amount / maxValue).toFloat().coerceIn(0f, 1f)
                        val barHeight = size.height * 0.72f * fraction
                        val left = gap + index * (barWidth + gap)
                        drawRoundRect(
                            color = barColors[index % barColors.size],
                            topLeft = Offset(left, size.height * 0.78f - barHeight),
                            size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                            cornerRadius = CornerRadius(barWidth * 0.2f, barWidth * 0.2f)
                        )
                    }
                }
                segments.forEachIndexed { index, (label, amount) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Canvas(modifier = Modifier.height(12.dp).fillMaxWidth(0.06f)) {
                                drawRoundRect(
                                    color = barColors[index % barColors.size],
                                    size = Size(size.width, size.height),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = amount.asCurrency(),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
