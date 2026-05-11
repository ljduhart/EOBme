package com.eobme.app.ui.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eobme.app.EOBmeApp
import com.eobme.app.R
import com.eobme.app.data.local.entity.CptRecordEntity
import com.eobme.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultsScreen(
    eobId: Long,
    app: EOBmeApp,
    onAppeal: () -> Unit,
    onBack: () -> Unit
) {
    val eob by app.eobRepository.observeEob(eobId).collectAsState(initial = null)
    val cptRecords by produceState<List<CptRecordEntity>>(initialValue = emptyList(), eobId) {
        value = app.cptRepository.getByEob(eobId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analysis_results)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        val currentEob = eob
        if (currentEob == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.loading))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.eob_details),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(stringResource(R.string.insurance_name), currentEob.insuranceName.ifBlank { "-" })
                            DetailRow(stringResource(R.string.provider_name), currentEob.providerName.ifBlank { "-" })
                            DetailRow(stringResource(R.string.date_of_service), DateUtils.formatDate(currentEob.dateOfService))
                            DetailRow(stringResource(R.string.upload_source), currentEob.uploadSource)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.financial_summary),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(stringResource(R.string.billed_amount), "$${String.format("%.2f", currentEob.billedAmount)}")
                            DetailRow(stringResource(R.string.insurance_paid_amount), "$${String.format("%.2f", currentEob.insurancePaid)}")
                            DetailRow(stringResource(R.string.contractual_adjustment), "$${String.format("%.2f", currentEob.contractualAdjustment)}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            DetailRow(stringResource(R.string.copay), "$${String.format("%.2f", currentEob.copay)}")
                            DetailRow(stringResource(R.string.deductible), "$${String.format("%.2f", currentEob.deductible)}")
                            DetailRow(stringResource(R.string.coinsurance), "$${String.format("%.2f", currentEob.coinsurance)}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            val totalPatientResp = currentEob.copay + currentEob.deductible + currentEob.coinsurance
                            DetailRow(
                                stringResource(R.string.total_patient_responsibility),
                                "$${String.format("%.2f", totalPatientResp)}",
                                bold = true
                            )
                        }
                    }
                }

                if (cptRecords.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.cpt_codes_found),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(cptRecords) { cpt ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cpt.code,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (cpt.description.isNotBlank()) {
                                        Text(
                                            text = cpt.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Text(
                                    text = cpt.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAppeal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.generate_appeal))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
