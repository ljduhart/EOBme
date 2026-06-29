package app.eob.me.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.FsaDoomsdayPhase
import app.eob.me.data.FsaDoomsdaySnapshot
import app.eob.me.data.ReceiptRecord
import app.eob.me.data.TaxVaultBudgetSummary
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.TaxVaultVisibilityMode
import app.eob.me.data.VaultEvidenceThumbnail
import app.eob.me.data.asCurrency
import app.eob.me.ui.components.home.TaxVaultVerticalFilterCard
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
    eligibleEobs: List<EobRecord>,
    vaultReceipts: List<ReceiptRecord>,
    selectedEobIds: Set<String>,
    selectedReceiptIds: Set<String>,
    onFilterSelected: (TaxVaultFilterState) -> Unit,
    onVisibilityModeSelected: (TaxVaultVisibilityMode) -> Unit,
    onToggleExportEob: (EobRecord) -> Unit,
    onToggleExportReceipt: (ReceiptRecord) -> Unit,
    onExportClaimPackage: () -> Unit,
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
                eligibleEobs = eligibleEobs,
                vaultReceipts = vaultReceipts,
                selectedEobIds = selectedEobIds,
                selectedReceiptIds = selectedReceiptIds,
                onFilterSelected = onFilterSelected,
                onVisibilityModeSelected = onVisibilityModeSelected,
                onToggleExportEob = onToggleExportEob,
                onToggleExportReceipt = onToggleExportReceipt,
                onExportClaimPackage = onExportClaimPackage,
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
    eligibleEobs: List<EobRecord>,
    vaultReceipts: List<ReceiptRecord>,
    selectedEobIds: Set<String>,
    selectedReceiptIds: Set<String>,
    onFilterSelected: (TaxVaultFilterState) -> Unit,
    onVisibilityModeSelected: (TaxVaultVisibilityMode) -> Unit,
    onToggleExportEob: (EobRecord) -> Unit,
    onToggleExportReceipt: (ReceiptRecord) -> Unit,
    onExportClaimPackage: () -> Unit,
    onAddReceipt: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddReceipt,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = EobStrings.t(language, "taxVaultAddReceipt")
                    )
                },
                text = {
                    Text(
                        text = EobStrings.t(language, "taxVaultAddReceipt"),
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = Color(0xFF7AD7FF),
                contentColor = Color(0xFF0B1F45),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            )
        }
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
            if (evidenceThumbnails.isNotEmpty()) {
                VaultEvidenceCarousel(language = language, thumbnails = evidenceThumbnails)
            }
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
                    .height(280.dp)
            )
            VaultExportSection(
                language = language,
                eligibleEobs = eligibleEobs,
                vaultReceipts = vaultReceipts,
                selectedEobIds = selectedEobIds,
                selectedReceiptIds = selectedReceiptIds,
                onToggleExportEob = onToggleExportEob,
                onToggleExportReceipt = onToggleExportReceipt,
                onExportClaimPackage = onExportClaimPackage
            )
            Spacer(modifier = Modifier.height(88.dp))
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
private fun VaultEvidenceCarousel(
    language: AppLanguage,
    thumbnails: List<VaultEvidenceThumbnail>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = EobStrings.t(language, "taxVaultEvidenceGalleryTitle"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(thumbnails, key = { it.id }) { thumbnail ->
                PolaroidEvidenceThumbnail(thumbnail = thumbnail)
            }
        }
    }
}

@Composable
private fun PolaroidEvidenceThumbnail(thumbnail: VaultEvidenceThumbnail) {
    Card(
        modifier = Modifier
            .width(132.dp)
            .graphicsLayer { rotationZ = thumbnail.rotationDegrees },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (thumbnail.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = thumbnail.imageUrl,
                    contentDescription = thumbnail.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(Color(0xFFECEFF1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = thumbnail.label.take(12),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF455A64)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = thumbnail.label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF263238),
                maxLines = 2
            )
        }
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
    onExportClaimPackage: () -> Unit
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
            eligibleEobs.take(8).forEach { record ->
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
            vaultReceipts.take(4).forEach { receipt ->
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
        }
    }
}
