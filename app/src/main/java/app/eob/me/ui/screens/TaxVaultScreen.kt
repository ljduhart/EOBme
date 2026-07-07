package app.eob.me.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import androidx.compose.ui.text.font.FontWeight
import app.eob.me.data.EobCharge
import app.eob.me.data.EobStrings
import app.eob.me.data.EobRecord
import app.eob.me.data.VaultEvidencePreviewDetail
import app.eob.me.data.FsaDoomsdayPhase
import app.eob.me.data.FsaDoomsdaySnapshot
import app.eob.me.data.ReceiptRecord
import app.eob.me.data.TaxVaultBudgetSummary
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.TaxVaultVisibilityMode
import app.eob.me.data.VaultEvidenceThumbnail
import app.eob.me.data.VaultSubstantiationStatus
import app.eob.me.data.asCurrency
import app.eob.me.ui.components.home.TaxVaultVerticalFilterCard
import app.eob.me.ui.components.taxvault.VaultAddReceiptButton
import app.eob.me.ui.components.taxvault.VaultEvidenceCarousel
import coil.compose.AsyncImage

private val VaultInteriorBackground = Brush.verticalGradient(
    colors = listOf(Color(0xFF0D1118), Color(0xFF1A1D22), Color(0xFF10141B))
)
private val TitaniumPanel = Color(0xFF2A3038)

@Composable
fun TaxVaultScreen(
    language: AppLanguage,
    darkModeEnabled: Boolean,
    isGoldTier: Boolean,
    doorAnimating: Boolean,
    filterState: TaxVaultFilterState,
    visibilityMode: TaxVaultVisibilityMode,
    budgetSummary: TaxVaultBudgetSummary,
    fsaSnapshot: FsaDoomsdaySnapshot,
    evidenceThumbnails: List<VaultEvidenceThumbnail>,
    evidencePreviewDetail: VaultEvidencePreviewDetail?,
    eligibleEobs: List<EobRecord>,
    vaultReceipts: List<ReceiptRecord>,
    selectedEobIds: Set<String>,
    selectedReceiptIds: Set<String>,
    onFilterSelected: (TaxVaultFilterState) -> Unit,
    onVisibilityModeSelected: (TaxVaultVisibilityMode) -> Unit,
    onToggleExportEob: (EobRecord) -> Unit,
    onToggleExportReceipt: (ReceiptRecord) -> Unit,
    onExportClaimPackage: () -> Unit,
    onEvidenceSelected: (String) -> Unit,
    onDismissEvidencePreview: () -> Unit,
    onAddReceipt: () -> Unit,
    onDoorAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var doorsOpen by remember { mutableStateOf(!doorAnimating) }
    LaunchedEffect(doorAnimating) {
        if (doorAnimating) {
            kotlinx.coroutines.delay(350)
            doorsOpen = true
            onDoorAnimationComplete()
        }
    }
    DisposableEffect(Unit) {
        onDispose { onDismissEvidencePreview() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VaultInteriorBackground)
    ) {
        AnimatedVisibility(
            visible = !doorsOpen,
            exit = slideOutHorizontally(
                animationSpec = tween(700),
                targetOffsetX = { fullWidth -> -fullWidth / 2 }
            )
        ) {
            VaultDoorPanels(language = language)
        }

        AnimatedVisibility(
            visible = doorsOpen,
            enter = slideInHorizontally(
                animationSpec = tween(700),
                initialOffsetX = { fullWidth -> fullWidth }
            )
        ) {
            TaxVaultDashboard(
                language = language,
                darkModeEnabled = darkModeEnabled,
                isGoldTier = isGoldTier,
                filterState = filterState,
                visibilityMode = visibilityMode,
                budgetSummary = budgetSummary,
                fsaSnapshot = fsaSnapshot,
                evidenceThumbnails = evidenceThumbnails,
                evidencePreviewDetail = evidencePreviewDetail,
                eligibleEobs = eligibleEobs,
                vaultReceipts = vaultReceipts,
                selectedEobIds = selectedEobIds,
                selectedReceiptIds = selectedReceiptIds,
                onFilterSelected = onFilterSelected,
                onVisibilityModeSelected = onVisibilityModeSelected,
                onToggleExportEob = onToggleExportEob,
                onToggleExportReceipt = onToggleExportReceipt,
                onExportClaimPackage = onExportClaimPackage,
                onEvidenceSelected = onEvidenceSelected,
                onDismissEvidencePreview = onDismissEvidencePreview,
                onAddReceipt = onAddReceipt
            )
        }
    }
}

@Composable
private fun VaultDoorPanels(language: AppLanguage) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1D22)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF3B424C), Color(0xFF1A1D22))
                    )
                )
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1A1D22), Color(0xFF3B424C))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = EobStrings.t(language, "taxVaultActivating"),
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TaxVaultDashboard(
    language: AppLanguage,
    darkModeEnabled: Boolean,
    isGoldTier: Boolean,
    filterState: TaxVaultFilterState,
    visibilityMode: TaxVaultVisibilityMode,
    budgetSummary: TaxVaultBudgetSummary,
    fsaSnapshot: FsaDoomsdaySnapshot,
    evidenceThumbnails: List<VaultEvidenceThumbnail>,
    evidencePreviewDetail: VaultEvidencePreviewDetail?,
    eligibleEobs: List<EobRecord>,
    vaultReceipts: List<ReceiptRecord>,
    selectedEobIds: Set<String>,
    selectedReceiptIds: Set<String>,
    onFilterSelected: (TaxVaultFilterState) -> Unit,
    onVisibilityModeSelected: (TaxVaultVisibilityMode) -> Unit,
    onToggleExportEob: (EobRecord) -> Unit,
    onToggleExportReceipt: (ReceiptRecord) -> Unit,
    onExportClaimPackage: () -> Unit,
    onEvidenceSelected: (String) -> Unit,
    onDismissEvidencePreview: () -> Unit,
    onAddReceipt: () -> Unit
) {
    val previewOpen = evidencePreviewDetail != null
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(if (previewOpen) Modifier.blur(12.dp) else Modifier),
            containerColor = Color.Transparent
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = EobStrings.t(language, "taxVaultDashboardTitle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            FsaDoomsdayMonitorCard(language = language, snapshot = fsaSnapshot)
            VaultEvidenceCarousel(
                language = language,
                thumbnails = evidenceThumbnails,
                onEvidenceSelected = onEvidenceSelected
            )
            TaxVaultVerticalFilterCard(
                language = language,
                darkModeEnabled = darkModeEnabled,
                isGoldTier = isGoldTier,
                filterState = filterState,
                visibilityMode = visibilityMode,
                budgetSummary = budgetSummary,
                onFilterSelected = onFilterSelected,
                onVisibilityModeSelected = onVisibilityModeSelected,
                onVaultDoorUnlocked = {},
                showTitaniumDoor = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
            VaultExportSection(
                language = language,
                eligibleEobs = eligibleEobs,
                vaultReceipts = vaultReceipts,
                selectedEobIds = selectedEobIds,
                selectedReceiptIds = selectedReceiptIds,
                onToggleExportEob = onToggleExportEob,
                onToggleExportReceipt = onToggleExportReceipt,
                onExportClaimPackage = onExportClaimPackage,
                onAddReceipt = onAddReceipt
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
        if (evidencePreviewDetail != null) {
            VaultEvidencePreviewOverlay(
                language = language,
                detail = evidencePreviewDetail,
                selectedEobIds = selectedEobIds,
                selectedReceiptIds = selectedReceiptIds,
                onDismiss = onDismissEvidencePreview,
                onToggleExportEob = onToggleExportEob,
                onToggleExportReceipt = onToggleExportReceipt
            )
        }
    }
}

@Composable
private fun FsaDoomsdayMonitorCard(
    language: AppLanguage,
    snapshot: FsaDoomsdaySnapshot
) {
    val containerColor = when (snapshot.phase) {
        FsaDoomsdayPhase.GREEN -> Color(0xFF1B5E20)
        FsaDoomsdayPhase.ORANGE -> Color(0xFFE65100)
        FsaDoomsdayPhase.RED -> Color(0xFFB71C1C)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = when (snapshot.phase) {
                FsaDoomsdayPhase.GREEN -> EobStrings.tf(
                    language,
                    snapshot.messageKey,
                    snapshot.eligibleClaimAmount.asCurrency()
                )
                FsaDoomsdayPhase.ORANGE -> EobStrings.tf(
                    language,
                    snapshot.messageKey,
                    snapshot.daysRemaining,
                    snapshot.unspentAmount.asCurrency()
                )
                FsaDoomsdayPhase.RED -> EobStrings.tf(
                    language,
                    snapshot.messageKey,
                    snapshot.daysRemaining
                )
            },
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun VaultExportSection(
    language: AppLanguage,
    eligibleEobs: List<EobRecord>,
    vaultReceipts: List<ReceiptRecord>,
    selectedEobIds: Set<String>,
    selectedReceiptIds: Set<String>,
    onToggleExportEob: (EobRecord) -> Unit,
    onToggleExportReceipt: (ReceiptRecord) -> Unit,
    onExportClaimPackage: () -> Unit,
    onAddReceipt: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TitaniumPanel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = EobStrings.t(language, "taxVaultExportTitle"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            eligibleEobs.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExportEob(record) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = record.historyListKey() in selectedEobIds,
                        onCheckedChange = { onToggleExportEob(record) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = record.providerName, color = Color.White)
                        if (VaultSubstantiationStatus.fromFirestore(record.vaultSubstantiationStatus) ==
                            VaultSubstantiationStatus.PAID_AND_SUBSTANTIATED
                        ) {
                            Text(
                                text = EobStrings.t(language, "taxVaultSubstantiated"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF3DDC84)
                            )
                        }
                        Text(
                            text = EobStrings.tf(
                                language,
                                "taxVaultExportEobLine",
                                record.serviceDate,
                                record.totalPatientResponsibility.asCurrency()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
            vaultReceipts.forEach { receipt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExportReceipt(receipt) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = receipt.historyListKey() in selectedReceiptIds,
                        onCheckedChange = { onToggleExportReceipt(receipt) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = receipt.providerName, color = Color.White)
                        Text(
                            text = EobStrings.tf(
                                language,
                                "taxVaultExportReceiptLine",
                                receipt.serviceDate,
                                receipt.amount.asCurrency()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF3DDC84))
                    .clickable(onClick = onExportClaimPackage)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    tint = Color(0xFF0B1F45)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = EobStrings.t(language, "taxVaultExportAction"),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B1F45)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                VaultAddReceiptButton(
                    language = language,
                    onClick = onAddReceipt
                )
            }
        }
    }
}

@Composable
private fun VaultEvidencePreviewOverlay(
    language: AppLanguage,
    detail: VaultEvidencePreviewDetail,
    selectedEobIds: Set<String>,
    selectedReceiptIds: Set<String>,
    onDismiss: () -> Unit,
    onToggleExportEob: (EobRecord) -> Unit,
    onToggleExportReceipt: (ReceiptRecord) -> Unit
) {
    var revealed by remember(detail) { mutableStateOf(false) }
    LaunchedEffect(detail) {
        revealed = false
        revealed = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(onClick = onDismiss)
        )
        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn(animationSpec = tween(220)) + scaleIn(
                initialScale = 0.88f,
                animationSpec = tween(280)
            ),
            exit = fadeOut(animationSpec = tween(180)) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(180)
            ),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
            ) {
                when (detail) {
                    is VaultEvidencePreviewDetail.Eob -> VaultEvidenceEobPreviewContent(
                        language = language,
                        record = detail.record,
                        includeInExport = detail.record.historyListKey() in selectedEobIds,
                        onToggleExport = { onToggleExportEob(detail.record) },
                        onDismiss = onDismiss
                    )
                    is VaultEvidencePreviewDetail.Receipt -> VaultEvidenceReceiptPreviewContent(
                        language = language,
                        receipt = detail.record,
                        includeInExport = detail.record.historyListKey() in selectedReceiptIds,
                        onToggleExport = { onToggleExportReceipt(detail.record) },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultEvidenceEobPreviewContent(
    language: AppLanguage,
    record: EobRecord,
    includeInExport: Boolean,
    onToggleExport: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.providerName.ifBlank { EobStrings.t(language, "provider") },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF102A43),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = onDismiss) {
                Text(EobStrings.t(language, "close"))
            }
        }
        if (record.storageDownloadUrl.isNotBlank()) {
            AsyncImage(
                model = record.storageDownloadUrl,
                contentDescription = record.providerName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        VaultEvidenceDetailLine(
            label = EobStrings.t(language, "provider"),
            value = record.providerName
        )
        VaultEvidenceDetailLine(
            label = EobStrings.t(language, "insurance"),
            value = record.insuranceName
        )
        VaultEvidenceDetailLine(
            label = EobStrings.t(language, "appointmentDate"),
            value = record.serviceDate
        )
        VaultEvidenceDetailLine(
            label = EobStrings.t(language, "patientResponsibility"),
            value = record.totalPatientResponsibility.asCurrency()
        )
        if (record.isHsaEligible || record.isFsaEligible) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (record.isHsaEligible) {
                    VaultEvidenceEligibilityChip(
                        text = EobStrings.t(language, "taxVaultHsaEligibleChip")
                    )
                }
                if (record.isFsaEligible) {
                    VaultEvidenceEligibilityChip(
                        text = EobStrings.t(language, "taxVaultFsaEligibleChip")
                    )
                }
            }
        }
        if (VaultSubstantiationStatus.fromFirestore(record.vaultSubstantiationStatus) ==
            VaultSubstantiationStatus.PAID_AND_SUBSTANTIATED
        ) {
            Text(
                text = EobStrings.t(language, "taxVaultSubstantiated"),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF1B5E20),
                fontWeight = FontWeight.SemiBold
            )
        }
        if (record.charges.isNotEmpty()) {
            Text(
                text = EobStrings.t(language, "historyProcedureCodes"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF546E7A)
            )
            record.charges.forEach { charge ->
                VaultEvidenceChargeLine(language = language, charge = charge)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExport),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = includeInExport,
                onCheckedChange = { onToggleExport() }
            )
            Text(
                text = EobStrings.t(language, "taxVaultEvidencePreviewIncludeExport"),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF263238)
            )
        }
    }
}

@Composable
private fun VaultEvidenceReceiptPreviewContent(
    language: AppLanguage,
    receipt: ReceiptRecord,
    includeInExport: Boolean,
    onToggleExport: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = EobStrings.t(language, "taxVaultEvidencePreviewReceiptTitle"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF102A43),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(EobStrings.t(language, "close"))
            }
        }
        if (receipt.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = receipt.thumbnailUrl,
                contentDescription = receipt.providerName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        VaultEvidenceDetailLine(
            label = EobStrings.t(language, "provider"),
            value = receipt.providerName
        )
        VaultEvidenceDetailLine(
            label = EobStrings.t(language, "appointmentDate"),
            value = receipt.serviceDate
        )
        VaultEvidenceDetailLine(
            label = EobStrings.t(language, "patientResponsibility"),
            value = receipt.amount.asCurrency()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExport),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = includeInExport,
                onCheckedChange = { onToggleExport() }
            )
            Text(
                text = EobStrings.t(language, "taxVaultEvidencePreviewIncludeExport"),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF263238)
            )
        }
    }
}

@Composable
private fun VaultEvidenceDetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF78909C)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF263238),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VaultEvidenceEligibilityChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(Color(0xFFE8F5E9), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF1B5E20),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun VaultEvidenceChargeLine(
    language: AppLanguage,
    charge: EobCharge
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = EobStrings.tf(language, "cptCodeLabel", charge.cptCode),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF263238)
            )
            if (charge.cptDescription.isNotBlank()) {
                Text(
                    text = charge.cptDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF607D8B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = charge.billedAmount.asCurrency(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF263238)
        )
    }
}
