package app.eob.me.ui.components.bento

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.CptBentoSnapshot
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.HistoryBentoSnapshot
import app.eob.me.data.InsuranceNewsBentoSnapshot
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.data.ProviderAvatarPreview
import app.eob.me.data.ProviderDirectoryAssurance
import app.eob.me.data.YtdBentoViewMode
import app.eob.me.data.YtdDeductibleBentoSnapshot
import app.eob.me.navigation.HubBentoDestination

@Composable
fun BentoGridCell(
    language: AppLanguage,
    destination: HubBentoDestination,
    historySnapshot: HistoryBentoSnapshot,
    processingPhase: InvoiceProcessingPhase,
    isLoadingInvoice: Boolean,
    historyFilter: HistoryBentoFilter,
    providerAvatars: List<ProviderAvatarPreview>,
    providerDirectoryAssurance: ProviderDirectoryAssurance,
    cptBentoSnapshot: CptBentoSnapshot,
    insuranceNewsBentoSnapshot: InsuranceNewsBentoSnapshot,
    ytdBentoSnapshot: YtdDeductibleBentoSnapshot,
    ytdBentoViewMode: YtdBentoViewMode,
    onYtdViewModeSelected: (YtdBentoViewMode) -> Unit,
    onClick: () -> Unit,
    onHistoryFilterSelected: (HistoryBentoFilter) -> Unit,
    onInvoiceFileDropFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (destination) {
        HubBentoDestination.EobHistory -> {
            HistoryBentoCell(
                language = language,
                snapshot = historySnapshot,
                processingPhase = processingPhase,
                isLoadingInvoice = isLoadingInvoice,
                activeFilter = historyFilter,
                onClick = onClick,
                onFilterSelected = onHistoryFilterSelected,
                onFileDropAnimationFinished = onInvoiceFileDropFinished,
                modifier = modifier
            )
        }
        HubBentoDestination.ProviderDirectory -> {
            ProviderDirectoryBentoCell(
                language = language,
                avatars = providerAvatars,
                directoryAssurance = providerDirectoryAssurance,
                onClick = onClick,
                modifier = modifier
            )
        }
        HubBentoDestination.CptTracker -> {
            CptTrackerBentoCell(
                language = language,
                snapshot = cptBentoSnapshot,
                onClick = onClick,
                modifier = modifier
            )
        }
        HubBentoDestination.YtdExpense -> {
            YtdExpenseBentoCell(
                language = language,
                snapshot = ytdBentoSnapshot,
                viewMode = ytdBentoViewMode,
                onViewModeSelected = onYtdViewModeSelected,
                onClick = onClick,
                modifier = modifier
            )
        }
        HubBentoDestination.InsuranceNews -> {
            InsuranceNewsBentoCell(
                language = language,
                snapshot = insuranceNewsBentoSnapshot,
                onClick = onClick,
                modifier = modifier
            )
        }
        else -> {
            DefaultBentoGridCell(
                language = language,
                destination = destination,
                onClick = onClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DefaultBentoGridCell(
    language: AppLanguage,
    destination: HubBentoDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BentoCellLayout.ASPECT_RATIO)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HubBentoIcon(
                destination = destination,
                tint = iconTint,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = destination.title(language),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
