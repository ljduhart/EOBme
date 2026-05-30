package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eob.me.data.YearlyHealthCostSummary
import app.eob.me.data.asCurrency

@Composable
fun YearlyHealthCostDashboard(summary: YearlyHealthCostSummary) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Yearly Health Cost Dashboard", style = MaterialTheme.typography.titleLarge)
            Text("Year: ${if (summary.year == 0) "No EOBs yet" else summary.year} • EOBs: ${summary.eobCount}")
            AmountRow("Total billed", summary.totalBilled)
            AmountRow("Insurance paid", summary.totalInsurancePaid)
            AmountRow("Contractual adjustments", summary.totalContractualAdjustment)
            AmountRow("Patient responsibility", summary.totalPatientResponsibility)
            AmountRow("Copays", summary.totalCopay)
            AmountRow("Deductibles", summary.totalDeductible)
            AmountRow("Coinsurance", summary.totalCoinsurance)
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
