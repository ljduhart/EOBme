package app.eob.me.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.DashboardFinancialMetrics
import app.eob.me.data.DashboardProviderCostRow
import app.eob.me.data.EobStrings
import app.eob.me.ui.components.HolographicGlassCard
import java.util.Locale

private val BilledBlue = Color(0xFF2498EA)
private val AdjustmentGreen = Color(0xFF10B981)
private val PatientRed = Color(0xFFEF4444)

@Composable
fun DashboardScreen(
    language: AppLanguage,
    metrics: DashboardFinancialMetrics,
    providerBreakdown: List<DashboardProviderCostRow>,
    recordCount: Int,
    modifier: Modifier = Modifier
) {

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

        if (recordCount == 0) {
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
                        color = Color.Gray
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        val total = metrics.grossBilled
                        if (total > 0) {
                            val adjustmentWeight = (metrics.adjustments / total).toFloat().coerceAtLeast(0.05f)
                            val insuranceWeight = (metrics.insurancePaid / total).toFloat().coerceAtLeast(0.05f)
                            val patientWeight = (metrics.patientDue / total).toFloat().coerceAtLeast(0.05f)

                            Box(
                                modifier = Modifier
                                    .weight(adjustmentWeight)
                                    .fillMaxHeight()
                                    .background(AdjustmentGreen)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(insuranceWeight)
                                    .fillMaxHeight()
                                    .background(BilledBlue)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(patientWeight)
                                    .fillMaxHeight()
                                    .background(PatientRed)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LegendItem(
                        label = EobStrings.t(language, "networkSavingsAdjustments"),
                        amount = metrics.adjustments,
                        color = AdjustmentGreen
                    )
                    LegendItem(
                        label = EobStrings.t(language, "coveredByCarrierPlan"),
                        amount = metrics.insurancePaid,
                        color = BilledBlue
                    )
                    LegendItem(
                        label = EobStrings.t(language, "yourPatientResponsibility"),
                        amount = metrics.patientDue,
                        color = PatientRed
                    )
                }
            }

            item {
                Text(
                    text = EobStrings.t(language, "spendingByFacility"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(providerBreakdown, key = { it.name }) { summary ->
                val maxValue = providerBreakdown.firstOrNull()?.totalAmount ?: 1.0
                val proportionalFill = (summary.totalAmount / maxValue).toFloat().coerceIn(0f, 1f)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = summary.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f", summary.totalAmount),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { proportionalFill },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = BilledBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = EobStrings.tf(language, "patientOutOfPocketShare", summary.patientAmount),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (summary.patientAmount > 0) PatientRed else Color.Gray
                        )
                    }
                }
            }
        }
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
            text = String.format(Locale.US, "$%.2f", amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

