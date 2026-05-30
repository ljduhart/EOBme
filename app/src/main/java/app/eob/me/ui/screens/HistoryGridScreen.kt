package app.eob.me.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.asCurrency
import app.eob.me.ui.components.HolographicGlassCard
import app.eob.me.ui.history.HistoryPagination
import app.eob.me.ui.theme.EobBlue40

@Composable
fun HistoryGridScreen(
    language: AppLanguage,
    records: List<EobRecord>,
    selectedRecord: EobRecord?,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onSelected: (EobRecord) -> Unit,
    onDeleteEob: (EobRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedRecords = records.sortedByDescending { it.serviceDateSortKey }
    val pageCount = HistoryPagination.availablePageCount(sortedRecords.size)
    val safePage = currentPage.coerceIn(0, pageCount - 1)

    LaunchedEffect(sortedRecords.size, pageCount) {
        if (safePage >= pageCount) {
            onPageChange((pageCount - 1).coerceAtLeast(0))
        }
    }

    val pageRecords = HistoryPagination.pageRecords(sortedRecords, safePage)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = EobStrings.t(language, "history"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "${sortedRecords.size} ${EobStrings.t(language, "eobs")} • Page ${safePage + 1} of $pageCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        if (sortedRecords.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Scan an EOB with the camera button to build your history grid.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(HistoryPagination.GRID_COLUMNS),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pageRecords, key = { it.id }) { record ->
                    EobSmartCard(
                        record = record,
                        isSelected = selectedRecord?.id == record.id,
                        onClick = { onSelected(record) },
                        onDelete = { onDeleteEob(record) }
                    )
                }
            }

            selectedRecord?.let { record ->
                EobSmartCardDetailPanel(language = language, record = record)
            }

            HistoryPageSelector(
                language = language,
                currentPage = safePage,
                pageCount = pageCount,
                onPageChange = onPageChange
            )
        }
    }
}

@Composable
private fun EobSmartCard(
    record: EobRecord,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isSelected) EobBlue40 else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                Color.White
            }
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = record.providerName.take(12).ifBlank { "—" },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = record.serviceDate,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = record.totalBilledAmount.asCurrency(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("✕", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EobSmartCardDetailPanel(language: AppLanguage, record: EobRecord) {
    HolographicGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Text(
            text = record.providerName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${EobStrings.t(language, "insurance")}: ${record.insuranceName}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailMetricRow(EobStrings.t(language, "billed"), record.totalBilledAmount.asCurrency())
        DetailMetricRow("Contractual adj.", record.totalContractualAdjustmentAmount.asCurrency())
        DetailMetricRow(EobStrings.t(language, "patientResponsibility"), record.totalPatientResponsibility.asCurrency())
        DetailMetricRow(EobStrings.t(language, "paid"), record.totalInsurancePaidAmount.asCurrency())
    }
}

@Composable
private fun DetailMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryPageSelector(
    language: AppLanguage,
    currentPage: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { onPageChange((currentPage - 1).coerceAtLeast(0)) },
            enabled = currentPage > 0
        ) {
            Text("Previous")
        }
        Text(
            text = "Page ${currentPage + 1} / $pageCount",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedButton(
            onClick = { onPageChange((currentPage + 1).coerceAtMost(pageCount - 1)) },
            enabled = currentPage < pageCount - 1
        ) {
            Text("Next")
        }
    }
}
