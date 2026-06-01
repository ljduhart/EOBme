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
