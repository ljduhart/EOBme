package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.ProviderSummary
import app.eob.me.data.asCurrency
import app.eob.me.ui.components.HolographicGlassCard

@Composable
fun ProviderDirectoryScreen(
    language: AppLanguage,
    providers: List<ProviderSummary>,
    modifier: Modifier = Modifier
) {
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
                color = Color.Gray
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(provider.providerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    }
                }
            }
        }
    }
}
