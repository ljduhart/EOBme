package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.ProviderSummary
import app.eob.me.data.asCurrency
import app.eob.me.ui.components.HolographicGlassCard

@Composable
fun ProviderDirectoryScreen(
    language: AppLanguage,
    providers: List<ProviderSummary>,
    records: List<EobRecord>,
    onDeleteEob: (EobRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val recordsByProvider = remember(records) {
        records
            .filter { it.providerName.isNotBlank() && !it.providerName.contains("not recognized", ignoreCase = true) }
            .groupBy { it.providerName }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 88.dp, top = 12.dp)
    ) {
        item {
            Text(
                text = EobStrings.t(language, "providerDirectoryTitle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = EobStrings.t(language, "providerDirectorySubtitle"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (providers.isEmpty()) {
            item {
                HolographicGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = EobStrings.t(language, "providerDirectoryEmpty"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(providers, key = { it.providerName }) { provider ->
                ProviderDirectoryCard(
                    language = language,
                    provider = provider,
                    providerRecords = recordsByProvider[provider.providerName].orEmpty()
                        .sortedByDescending { it.serviceDateSortKey },
                    onDeleteEob = onDeleteEob
                )
            }
        }
    }
}

@Composable
private fun ProviderDirectoryCard(
    language: AppLanguage,
    provider: ProviderSummary,
    providerRecords: List<EobRecord>,
    onDeleteEob: (EobRecord) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    provider.providerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        if (expanded) {
                            EobStrings.t(language, "providerHideEobs")
                        } else {
                            EobStrings.tf(language, "providerShowEobs", provider.eobCount)
                        }
                    )
                }
            }
            Text(
                EobStrings.tf(
                    language,
                    "providerEobSummary",
                    provider.eobCount,
                    provider.lastServiceDate
                )
            )
            Text(
                EobStrings.tf(
                    language,
                    "providerBilledPaid",
                    provider.totalBilled.asCurrency(),
                    provider.totalInsurancePaid.asCurrency()
                )
            )
            Text(
                text = "${EobStrings.t(language, "patientResponsibility")}: ${
                    provider.totalPatientResponsibility.asCurrency()
                }",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )

            if (expanded) {
                providerRecords.forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.serviceDate,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = record.totalBilledAmount.asCurrency(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(onClick = { onDeleteEob(record) }) {
                            Text(
                                text = EobStrings.t(language, "deleteEob"),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
