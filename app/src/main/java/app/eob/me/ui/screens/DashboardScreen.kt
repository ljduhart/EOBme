package app.eob.me.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.ClaimStatus
import app.eob.me.data.EobStrings
import app.eob.me.data.ExpenseAnalyticsAllocation
import app.eob.me.data.ExpenseAnalyticsSort
import app.eob.me.data.ExpenseAnalyticsState
import app.eob.me.data.FacilitySpending
import app.eob.me.data.MedicalClaim
import app.eob.me.data.asCurrency
import app.eob.me.ui.theme.EobExpenseBentoSurface
import app.eob.me.ui.theme.EobExpenseCarrierCovered
import app.eob.me.ui.theme.EobExpenseNetworkSavings
import app.eob.me.ui.theme.EobExpensePatientResponsibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    language: AppLanguage,
    state: ExpenseAnalyticsState,
    onBack: () -> Unit,
    onSortSelected: (ExpenseAnalyticsSort) -> Unit,
    onFacilityExpandedToggle: (String) -> Unit,
    onInspectClaimSource: (MedicalClaim) -> Unit,
    onAppealClaim: (MedicalClaim) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = EobStrings.t(language, "appBrand"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = EobStrings.t(language, "expenseAnalyticsBackCd")
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = EobStrings.t(language, "expenseAnalyticsLoading"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            state.allocation == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = EobStrings.t(language, "expenseAnalyticsEmpty"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                ExpenseAnalyticsContent(
                    language = language,
                    state = state,
                    onSortSelected = onSortSelected,
                    onFacilityExpandedToggle = onFacilityExpandedToggle,
                    onInspectClaimSource = onInspectClaimSource,
                    onAppealClaim = onAppealClaim
                )
            }
        }
    }
}

@Composable
private fun ExpenseAnalyticsContent(
    language: AppLanguage,
    state: ExpenseAnalyticsState,
    onSortSelected: (ExpenseAnalyticsSort) -> Unit,
    onFacilityExpandedToggle: (String) -> Unit,
    onInspectClaimSource: (MedicalClaim) -> Unit,
    onAppealClaim: (MedicalClaim) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = EobStrings.t(language, "expenseAnalytics"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ClaimAllocationSection(
                language = language,
                allocation = state.allocation!!,
                totalBilled = state.totalBilled
            )
        }
        item {
            SummaryBentoCard(language = language, state = state)
        }
        item {
            ExpenseAnalyticsSortChips(
                language = language,
                selectedSort = state.selectedSort,
                onSortSelected = onSortSelected
            )
        }
        item {
            Text(
                text = EobStrings.t(language, "expenseAnalyticsFacilitiesTitle"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        items(state.facilities, key = { facility -> facility.id }) { facility ->
            FacilitySpendingCard(
                language = language,
                facility = facility,
                onToggleExpanded = { onFacilityExpandedToggle(facility.id) },
                onInspectClaimSource = onInspectClaimSource,
                onAppealClaim = onAppealClaim
            )
        }
    }
}

@Composable
private fun ClaimAllocationSection(
    language: AppLanguage,
    allocation: ExpenseAnalyticsAllocation,
    totalBilled: Double
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = EobExpenseBentoSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = EobStrings.t(language, "expenseAnalyticsClaimAllocation"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = EobStrings.tf(language, "expenseAnalyticsTotalBilled", totalBilled.asCurrency()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ClaimAllocationBar(allocation = allocation)
            AllocationLegendRow(
                label = EobStrings.t(language, "expenseAnalyticsNetworkSavings"),
                amount = allocation.networkSavings,
                color = EobExpenseNetworkSavings
            )
            AllocationLegendRow(
                label = EobStrings.t(language, "expenseAnalyticsCarrierCovered"),
                amount = allocation.carrierCovered,
                color = EobExpenseCarrierCovered
            )
            AllocationLegendRow(
                label = EobStrings.t(language, "expenseAnalyticsPatientResponsibility"),
                amount = allocation.patientResponsibility,
                color = EobExpensePatientResponsibility
            )
        }
    }
}

@Composable
private fun ClaimAllocationBar(allocation: ExpenseAnalyticsAllocation) {
    val barShape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(barShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (allocation.networkSavingsFraction() > 0f) {
            Box(
                modifier = Modifier
                    .weight(allocation.networkSavingsFraction().coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(EobExpenseNetworkSavings)
            )
        }
        if (allocation.carrierCoveredFraction() > 0f) {
            Box(
                modifier = Modifier
                    .weight(allocation.carrierCoveredFraction().coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(EobExpenseCarrierCovered)
            )
        }
        if (allocation.patientResponsibilityFraction() > 0f) {
            Box(
                modifier = Modifier
                    .weight(allocation.patientResponsibilityFraction().coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(EobExpensePatientResponsibility)
            )
        }
    }
}

@Composable
private fun AllocationLegendRow(
    label: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = amount.asCurrency(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryBentoCard(
    language: AppLanguage,
    state: ExpenseAnalyticsState
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = EobExpenseBentoSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = EobStrings.t(language, "expenseAnalyticsSummaryTitle"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = EobStrings.t(language, "expenseAnalyticsTotalOopLabel"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = state.totalPatientOutOfPocket.asCurrency(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EobExpensePatientResponsibility
            )
            Text(
                text = EobStrings.tf(
                    language,
                    "expenseAnalyticsCarrierContribution",
                    state.totalCarrierContribution.asCurrency()
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = EobStrings.tf(
                    language,
                    "expenseAnalyticsNetworkSavingsDetail",
                    state.totalNetworkSavings.asCurrency()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = EobStrings.tf(
                    language,
                    "expenseAnalyticsTotalBilledDetail",
                    state.totalBilled.asCurrency()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpenseAnalyticsSortChips(
    language: AppLanguage,
    selectedSort: ExpenseAnalyticsSort,
    onSortSelected: (ExpenseAnalyticsSort) -> Unit
) {
    val sorts = listOf(
        ExpenseAnalyticsSort.HighestPatientShare to "expenseAnalyticsSortHighestPatientShare",
        ExpenseAnalyticsSort.NewestActivity to "expenseAnalyticsSortNewestActivity",
        ExpenseAnalyticsSort.HighestBilledTotal to "expenseAnalyticsSortHighestBilled",
        ExpenseAnalyticsSort.FacilityAlphabetical to "expenseAnalyticsSortAlphabetical"
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sorts) { (sort, labelKey) ->
            FilterChip(
                selected = selectedSort == sort,
                onClick = { onSortSelected(sort) },
                label = {
                    Text(
                        text = EobStrings.t(language, labelKey),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun FacilitySpendingCard(
    language: AppLanguage,
    facility: FacilitySpending,
    onToggleExpanded: () -> Unit,
    onInspectClaimSource: (MedicalClaim) -> Unit,
    onAppealClaim: (MedicalClaim) -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (facility.isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "facilityChevronRotation"
    )
    val carrierFraction = if (facility.totalSpent <= 0.0) {
        0f
    } else {
        (facility.carrierShare / facility.totalSpent).toFloat().coerceIn(0f, 1f)
    }
    val patientFraction = if (facility.totalSpent <= 0.0) {
        0f
    } else {
        (facility.outOfPocketShare / facility.totalSpent).toFloat().coerceIn(0f, 1f)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = EobExpenseBentoSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = facility.providerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = facility.totalSpent.asCurrency(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DualShareProgressBar(
                        carrierFraction = carrierFraction,
                        patientFraction = patientFraction
                    )
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = EobStrings.t(
                            language,
                            if (facility.isExpanded) {
                                "expenseAnalyticsCollapseCd"
                            } else {
                                "expenseAnalyticsExpandCd"
                            }
                        ),
                        modifier = Modifier.rotate(chevronRotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = facility.isExpanded,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    facility.claims.forEach { claim ->
                        ClaimEntryRow(
                            language = language,
                            claim = claim,
                            onInspectClaimSource = onInspectClaimSource,
                            onAppealClaim = onAppealClaim
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DualShareProgressBar(
    carrierFraction: Float,
    patientFraction: Float
) {
    val barShape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(barShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (carrierFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(carrierFraction.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(EobExpenseCarrierCovered)
            )
        }
        if (patientFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(patientFraction.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(EobExpensePatientResponsibility)
            )
        }
    }
}

@Composable
private fun ClaimEntryRow(
    language: AppLanguage,
    claim: MedicalClaim,
    onInspectClaimSource: (MedicalClaim) -> Unit,
    onAppealClaim: (MedicalClaim) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = EobStrings.tf(language, "expenseAnalyticsClaimLabel", claim.claimNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = EobStrings.tf(language, "expenseAnalyticsClaimDos", claim.dateOfService),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = EobStrings.tf(
                        language,
                        "expenseAnalyticsCarrierContribution",
                        claim.carrierCovered.asCurrency()
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = claim.totalBilled.asCurrency(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = { onInspectClaimSource(claim) }) {
                    Icon(
                        imageVector = Icons.Outlined.DocumentScanner,
                        contentDescription = EobStrings.t(language, "expenseAnalyticsScanCd")
                    )
                }
                Icon(
                    imageVector = claimStatusIcon(claim.status),
                    contentDescription = claimStatusContentDescription(language, claim.status),
                    tint = claimStatusTint(claim.status),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(horizontal = 8.dp)
                )
                IconButton(onClick = { onAppealClaim(claim) }) {
                    Icon(
                        imageVector = Icons.Filled.Gavel,
                        contentDescription = EobStrings.t(language, "expenseAnalyticsAppealCd"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (claim.status is ClaimStatus.PotentialError) {
            Text(
                text = claim.status.message,
                style = MaterialTheme.typography.bodySmall,
                color = EobExpensePatientResponsibility
            )
        }
    }
}

@Composable
private fun claimStatusIcon(status: ClaimStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        ClaimStatus.AuditedCorrect -> Icons.Outlined.Shield
        is ClaimStatus.PotentialError -> Icons.Filled.Warning
        ClaimStatus.Appealed -> Icons.Filled.Gavel
    }
}

private fun claimStatusContentDescription(language: AppLanguage, status: ClaimStatus): String {
    return when (status) {
        ClaimStatus.AuditedCorrect -> EobStrings.t(language, "expenseAnalyticsStatusAudited")
        is ClaimStatus.PotentialError -> EobStrings.t(language, "expenseAnalyticsStatusError")
        ClaimStatus.Appealed -> EobStrings.t(language, "expenseAnalyticsStatusAppealed")
    }
}

@Composable
private fun claimStatusTint(status: ClaimStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        ClaimStatus.AuditedCorrect -> EobExpenseCarrierCovered
        is ClaimStatus.PotentialError -> EobExpensePatientResponsibility
        ClaimStatus.Appealed -> MaterialTheme.colorScheme.primary
    }
}
