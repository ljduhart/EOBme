package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings

@Composable
fun HomeScreen(
    language: AppLanguage,
    profile: app.eob.me.data.UserProfile,
    records: List<EobRecord>,
    appointments: List<DoctorAppointment>,
    uploadNotice: String,
    onAddAppointment: (String, String, String) -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { InsuranceCard(language, profile) }
        item { YearlyHealthCostDashboard(EobAnalyzer.yearlyHealthCostSummary(records)) }
        item { ProviderDirectoryCard(EobAnalyzer.providerDirectory(records)) }
        item {
            QuickActionsCard(
                language = language,
                appointments = appointments,
                onAddAppointment = onAddAppointment,
                onRemoveAppointment = onRemoveAppointment
            )
        }
        if (uploadNotice.isNotBlank()) {
            item { Text(uploadNotice, style = MaterialTheme.typography.titleSmall) }
        }
        item {
            Text(
                "${EobStrings.t(language, "analysis")}: ${records.size} ${EobStrings.t(language, "eobs")}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
