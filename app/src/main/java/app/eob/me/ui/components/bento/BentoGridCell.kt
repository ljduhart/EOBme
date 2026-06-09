package app.eob.me.ui.components.bento

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    appealGeneratorBentoProcessing: Boolean,
    onClick: () -> Unit,
    onHistoryFilterSelected: (HistoryBentoFilter) -> Unit,
    onInvoiceFileDropFinished: () -> Unit,
    onAppealGeneratorProcessingFinished: () -> Unit,
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
        HubBentoDestination.AppealGenerator -> {
            AppealGeneratorBentoCell(
                language = language,
                isProcessing = appealGeneratorBentoProcessing,
                onClick = onClick,
                onProcessingAnimationFinished = onAppealGeneratorProcessingFinished,
                modifier = modifier
            )
        }
    }
}
