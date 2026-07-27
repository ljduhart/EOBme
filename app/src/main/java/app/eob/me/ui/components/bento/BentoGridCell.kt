package app.eob.me.ui.components.bento

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.eob.me.data.AppLanguage
import app.eob.me.data.CptBentoSnapshot
import app.eob.me.data.EobmeFeatureGate
import app.eob.me.data.FeatureAccess
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.HistoryBentoSnapshot
import app.eob.me.data.InsuranceNewsBentoSnapshot
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.data.TaxVaultBudgetSummary
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.ProviderAvatarPreview
import app.eob.me.data.ProviderDirectoryAssurance
import app.eob.me.data.YtdBentoViewMode
import app.eob.me.data.YtdDeductibleBentoSnapshot
import app.eob.me.data.SubscriptionTier
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.ui.components.PremiumBentoBox

@Composable
fun BentoGridCell(
    language: AppLanguage,
    destination: HubBentoDestination,
    historySnapshot: HistoryBentoSnapshot,
    taxVaultActive: Boolean = false,
    taxVaultBudgetSummary: TaxVaultBudgetSummary = TaxVaultBudgetSummary(0.0, 0.0),
    taxVaultFilterState: TaxVaultFilterState = TaxVaultFilterState.OFF,
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
    subscriptionTier: SubscriptionTier,
    cellAspectRatio: Float = BentoCellLayout.ASPECT_RATIO,
    onLockedClick: () -> Unit,
    onClick: () -> Unit,
    onHistoryFilterSelected: (HistoryBentoFilter) -> Unit,
    onInvoiceFileDropFinished: () -> Unit,
    onAppealGeneratorProcessingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnlocked = isBentoDestinationUnlocked(destination, subscriptionTier)
    val cellModifier = modifier
    val cellContent: @Composable () -> Unit = {
        BentoGridCellContent(
            language = language,
            destination = destination,
            cellAspectRatio = cellAspectRatio,
            historySnapshot = historySnapshot,
            taxVaultActive = taxVaultActive,
            taxVaultBudgetSummary = taxVaultBudgetSummary,
            taxVaultFilterState = taxVaultFilterState,
            processingPhase = processingPhase,
            isLoadingInvoice = isLoadingInvoice,
            historyFilter = historyFilter,
            providerAvatars = providerAvatars,
            providerDirectoryAssurance = providerDirectoryAssurance,
            cptBentoSnapshot = cptBentoSnapshot,
            insuranceNewsBentoSnapshot = insuranceNewsBentoSnapshot,
            ytdBentoSnapshot = ytdBentoSnapshot,
            ytdBentoViewMode = ytdBentoViewMode,
            onYtdViewModeSelected = onYtdViewModeSelected,
            appealGeneratorBentoProcessing = appealGeneratorBentoProcessing,
            onClick = onClick,
            onHistoryFilterSelected = onHistoryFilterSelected,
            onInvoiceFileDropFinished = onInvoiceFileDropFinished,
            onAppealGeneratorProcessingFinished = onAppealGeneratorProcessingFinished,
            billingErrorDetectionEnabled = EobmeFeatureGate.hasBillingErrorDetection(subscriptionTier),
            modifier = Modifier.fillMaxSize()
        )
    }

    if (destination.requiresPremiumGate()) {
        PremiumBentoBox(
            isUnlocked = isUnlocked,
            onLockedClick = onLockedClick,
            language = language,
            modifier = cellModifier,
            content = cellContent
        )
    } else {
        Box(modifier = cellModifier) {
            cellContent()
        }
    }
}

@Composable
private fun BentoGridCellContent(
    language: AppLanguage,
    destination: HubBentoDestination,
    cellAspectRatio: Float,
    historySnapshot: HistoryBentoSnapshot,
    taxVaultActive: Boolean,
    taxVaultBudgetSummary: TaxVaultBudgetSummary,
    taxVaultFilterState: TaxVaultFilterState,
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
    billingErrorDetectionEnabled: Boolean,
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
                cellAspectRatio = cellAspectRatio,
                taxVaultActive = taxVaultActive,
                taxVaultBudgetSummary = taxVaultBudgetSummary,
                taxVaultFilterState = taxVaultFilterState,
                processingPhase = processingPhase,
                isLoadingInvoice = isLoadingInvoice,
                activeFilter = historyFilter,
                billingErrorDetectionEnabled = billingErrorDetectionEnabled,
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
                cellAspectRatio = cellAspectRatio,
                onClick = onClick,
                modifier = modifier
            )
        }
        HubBentoDestination.CptTracker -> {
            CptTrackerBentoCell(
                language = language,
                snapshot = cptBentoSnapshot,
                cellAspectRatio = cellAspectRatio,
                onClick = onClick,
                modifier = modifier
            )
        }
        HubBentoDestination.YtdExpense -> {
            YtdExpenseBentoCell(
                language = language,
                snapshot = ytdBentoSnapshot,
                viewMode = ytdBentoViewMode,
                cellAspectRatio = cellAspectRatio,
                onViewModeSelected = onYtdViewModeSelected,
                onClick = onClick,
                modifier = modifier
            )
        }
        HubBentoDestination.InsuranceNews -> {
            InsuranceNewsBentoCell(
                language = language,
                snapshot = insuranceNewsBentoSnapshot,
                cellAspectRatio = cellAspectRatio,
                onClick = onClick,
                modifier = modifier
            )
        }
        HubBentoDestination.AppealGenerator -> {
            AppealGeneratorBentoCell(
                language = language,
                isProcessing = appealGeneratorBentoProcessing,
                cellAspectRatio = cellAspectRatio,
                onClick = onClick,
                onProcessingAnimationFinished = onAppealGeneratorProcessingFinished,
                modifier = modifier
            )
        }
    }
}

private fun HubBentoDestination.requiresPremiumGate(): Boolean = when (this) {
    HubBentoDestination.YtdExpense,
    HubBentoDestination.InsuranceNews,
    HubBentoDestination.AppealGenerator -> true
    else -> false
}

private fun isBentoDestinationUnlocked(
    destination: HubBentoDestination,
    tier: SubscriptionTier
): Boolean = when (destination) {
    HubBentoDestination.YtdExpense -> EobmeFeatureGate.hasYtdExpenseTracker(tier)
    HubBentoDestination.InsuranceNews -> EobmeFeatureGate.hasRealTimeNews(tier)
    HubBentoDestination.AppealGenerator ->
        EobmeFeatureGate.getAppealLetterLimit(tier) !is FeatureAccess.Denied
    else -> true
}
