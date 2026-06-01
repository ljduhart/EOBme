package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobStrings
import app.eob.me.data.EobRecord
import app.eob.me.ui.components.HolographicGlassCard

@Composable
fun YearlyExpenseScreen(
    language: AppLanguage,
    records: List<EobRecord>,
    modifier: Modifier = Modifier
) {
    val summary = remember(records) { EobAnalyzer.yearlyHealthCostSummary(records) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = EobStrings.t(language, "yearlyExpenseTitle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            HolographicGlassCard(modifier = Modifier.fillMaxWidth()) {
                YearlyHealthCostDashboard(language = language, summary = summary)
            }
        }
        item {
            YearlyHealthCostBarChart(language = language, summary = summary)
        }
    }
}
