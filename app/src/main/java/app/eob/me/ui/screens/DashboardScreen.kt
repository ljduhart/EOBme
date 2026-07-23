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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eob.me.R
import app.eob.me.data.ClaimStatus
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
                    text = stringResource(R.string.expense_analytics_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.expense_analytics_back_cd)
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
                            text = stringResource(R.string.expense_analytics_loading),
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
                        text = stringResource(R.string.expense_analytics_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                ExpenseAnalyticsContent(
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
                text = stringResource(R.string.expense_analytics_page_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ClaimAllocationSection(
                allocation = state.allocation!!,
                totalBilled = state.totalBilled
            )
        }
        item {
            SummaryBentoCard(state = state)
        }
        item {
            ExpenseAnalyticsSortChips(
                selectedSort = state.selectedSort,
                onSortSelected = onSortSelected
            )
        }
        item {
            Text(
                text = stringResource(R.string.expense_analytics_facilities_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        items(state.facilities, key = { facility -> facility.id }) { facility ->
            FacilitySpendingCard(
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
                text = stringResource(R.string.expense_analytics_claim_allocation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.expense_analytics_total_billed, totalBilled.asCurrency()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ClaimAllocationBar(allocation = allocation)
            AllocationLegendRow(
                label = stringResource(R.string.expense_analytics_network_savings),
                amount = allocation.networkSavings,
                color = EobExpenseNetworkSavings
            )
            AllocationLegendRow(
                label = stringResource(R.string.expense_analytics_carrier_covered),
                amount = allocation.carrierCovered,
                color = EobExpenseCarrierCovered
            )
            AllocationLegendRow(
                label = stringResource(R.string.expense_analytics_patient_responsibility),
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
private fun SummaryBentoCard(state: ExpenseAnalyticsState) {
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
                text = stringResource(R.string.expense_analytics_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.expense_analytics_total_oop_label),
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
                text = stringResource(
                    R.string.expense_analytics_carrier_contribution,
                    state.totalCarrierContribution.asCurrency()
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.expense_analytics_network_savings_detail,
                    state.totalNetworkSavings.asCurrency()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.expense_analytics_total_billed_detail,
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
    selectedSort: ExpenseAnalyticsSort,
    onSortSelected: (ExpenseAnalyticsSort) -> Unit
) {
    val sorts = listOf(
        ExpenseAnalyticsSort.HighestPatientShare to R.string.expense_analytics_sort_highest_patient_share,
        ExpenseAnalyticsSort.NewestActivity to R.string.expense_analytics_sort_newest_activity,
        ExpenseAnalyticsSort.HighestBilledTotal to R.string.expense_analytics_sort_highest_billed,
        ExpenseAnalyticsSort.FacilityAlphabetical to R.string.expense_analytics_sort_alphabetical
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sorts) { (sort, labelRes) ->
            FilterChip(
                selected = selectedSort == sort,
                onClick = { onSortSelected(sort) },
                label = {
                    Text(
                        text = stringResource(labelRes),
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
                        contentDescription = stringResource(
                            if (facility.isExpanded) {
                                R.string.expense_analytics_collapse_cd
                            } else {
                                R.string.expense_analytics_expand_cd
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
                    text = stringResource(R.string.expense_analytics_claim_label, claim.claimNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.expense_analytics_claim_dos, claim.dateOfService),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.expense_analytics_carrier_contribution,
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
                        contentDescription = stringResource(R.string.expense_analytics_scan_cd)
                    )
                }
                Icon(
                    imageVector = claimStatusIcon(claim.status),
                    contentDescription = claimStatusContentDescription(claim.status),
                    tint = claimStatusTint(claim.status),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(horizontal = 8.dp)
                )
                IconButton(onClick = { onAppealClaim(claim) }) {
                    Icon(
                        imageVector = Icons.Filled.Gavel,
                        contentDescription = stringResource(R.string.expense_analytics_appeal_cd),
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

@Composable
private fun claimStatusContentDescription(status: ClaimStatus): String {
    return when (status) {
        ClaimStatus.AuditedCorrect -> stringResource(R.string.expense_analytics_status_audited)
        is ClaimStatus.PotentialError -> stringResource(R.string.expense_analytics_status_error)
        ClaimStatus.Appealed -> stringResource(R.string.expense_analytics_status_appealed)
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
