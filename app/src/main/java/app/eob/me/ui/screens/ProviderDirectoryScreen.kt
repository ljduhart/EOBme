package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.ProviderSummary
import app.eob.me.ui.components.HolographicGlassCard

@Composable
fun ProviderDirectoryScreen(
    language: AppLanguage,
    providers: List<ProviderSummary>,
    records: List<EobRecord>,
    onViewProviderRecords: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val premiumProviders = remember(providers, records) {
        providers.map { summary ->
            summary.toPremiumProviderSummary(records)
        }
    }

    if (premiumProviders.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = EobStrings.t(language, "providerDirectoryTitle"),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = EobStrings.t(language, "providerDirectorySubtitle"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            HolographicGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = EobStrings.t(language, "providerDirectoryEmpty"),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        AnimatedProviderDirectoryScreen(
            providers = premiumProviders,
            onViewEobsClicked = onViewProviderRecords,
            modifier = modifier
        )
    }
}

internal fun ProviderSummary.toPremiumProviderSummary(records: List<EobRecord>): PremiumProviderSummary {
    val providerRecords = records.filter { EobAnalyzer.providerNamesEqual(it.providerName, providerName) }
    val latestRecord = providerRecords.maxByOrNull { it.serviceDateSortKey }
    val networkStatus = when {
        latestRecord == null -> NetworkStatus.PENDING
        EobAnalyzer.recordSignalsOutOfNetwork(latestRecord) -> NetworkStatus.OUT_OF_NETWORK
        else -> NetworkStatus.IN_NETWORK
    }
    return PremiumProviderSummary(
        id = providerName,
        name = providerName,
        eobCount = eobCount,
        lastServiceDate = lastServiceDate,
        totalBilled = totalBilled,
        totalPaid = totalInsurancePaid,
        totalResponsibility = totalPatientResponsibility,
        networkStatus = networkStatus
    )
}
