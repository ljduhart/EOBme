package app.eob.me.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.DoctorDisputeStrategy
import app.eob.me.data.EobCharge
import app.eob.me.data.EobHistoryPaymentFilter
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.HistoryTimelineSection
import app.eob.me.data.HistoryTimelineRow
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.asCurrency

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EobHistoryScreen(
    language: AppLanguage,
    timelineSections: List<HistoryTimelineSection>,
    paymentFilter: EobHistoryPaymentFilter,
    onPaymentFilterSelected: (EobHistoryPaymentFilter) -> Unit,
    onDeleteEob: (EobRecord) -> Unit,
    onUploadEob: () -> Unit,
    onRecordSelected: (EobRecord) -> Unit,
    selectedRecord: EobRecord? = null,
    onAppealDoctorWithStrategy: (EobRecord, DoctorDisputeStrategy) -> Unit = { _, _ -> },
    onAppealInsurance: (EobRecord) -> Unit = {},
    showVaultFilterBanner: Boolean = false,
    taxVaultFilterState: TaxVaultFilterState = TaxVaultFilterState.OFF,
    modifier: Modifier = Modifier
) {
    var expandedRecordKey by remember { mutableStateOf("") }
    var doctorAppealTargetRecord by remember { mutableStateOf<EobRecord?>(null) }
    val listState = rememberLazyListState()
    var fabExpanded by remember { mutableStateOf(true) }
    var previousFirstIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.collect { (index, offset, scrolling) ->
            if (scrolling) {
                val scrollingDown = index > previousFirstIndex ||
                    (index == previousFirstIndex && offset > previousScrollOffset)
                fabExpanded = !scrollingDown
            } else if (index == 0 && offset == 0) {
                fabExpanded = true
            }
            previousFirstIndex = index
            previousScrollOffset = offset
        }
    }

    val recordCount = remember(timelineSections) {
        timelineSections.sumOf { section -> section.rows.size }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = fabExpanded,
                onClick = onUploadEob,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = EobStrings.t(language, "uploadEob")
                    )
                },
                text = {
                    Text(
                        text = EobStrings.t(language, "uploadEob"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = EobStrings.t(language, "history"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$recordCount ${EobStrings.t(language, "eobs")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                HistoryPaymentFilterChips(
                    language = language,
                    selectedFilter = paymentFilter,
                    onFilterSelected = {
                        onPaymentFilterSelected(it)
                        expandedRecordKey = ""
                        doctorAppealTargetRecord = null
                    }
                )
            }

            if (showVaultFilterBanner) {
                Text(
                    text = EobStrings.t(language, "taxVaultFilteredBanner"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            Color(0xFF3DDC84).copy(alpha = 0.14f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B7F4B)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (recordCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = EobStrings.t(language, "historyEmptyHint"),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                HistoryTimelineList(
                    language = language,
                    timelineSections = timelineSections,
                    listState = listState,
                    expandedRecordKey = expandedRecordKey,
                    selectedRecord = selectedRecord,
                    taxVaultFilterState = taxVaultFilterState,
                    showVaultFilterBanner = showVaultFilterBanner,
                    onExpandToggle = { record ->
                        val recordKey = record.historyListKey()
                        val collapsingSame = expandedRecordKey == recordKey
                        if (!collapsingSame) {
                            onRecordSelected(record)
                            doctorAppealTargetRecord = null
                        }
                        expandedRecordKey = if (collapsingSame) "" else recordKey
                    },
                    onDoctorAppealRequested = { record ->
                        onRecordSelected(record)
                        doctorAppealTargetRecord = if (
                            doctorAppealTargetRecord?.matchesHistoryRecord(record) == true
                        ) {
                            null
                        } else {
                            record
                        }
                    },
                    onAppealInsurance = { record ->
                        doctorAppealTargetRecord = null
                        onAppealInsurance(record)
                    },
                    onDeleteEob = onDeleteEob
                )
            }
        }

            doctorAppealTargetRecord?.let { record ->
                if (selectedRecord?.matchesHistoryRecord(record) == true) {
                    DoctorAppealStrategyFloater(
                        language = language,
                        onDismiss = { doctorAppealTargetRecord = null },
                        onStrategySelected = { strategy ->
                            onAppealDoctorWithStrategy(record, strategy)
                            doctorAppealTargetRecord = null
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryPaymentFilterChips(
    language: AppLanguage,
    selectedFilter: EobHistoryPaymentFilter,
    onFilterSelected: (EobHistoryPaymentFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == EobHistoryPaymentFilter.All,
                onClick = { onFilterSelected(EobHistoryPaymentFilter.All) },
                label = { Text(EobStrings.t(language, "historyFilterAll")) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == EobHistoryPaymentFilter.Paid,
                onClick = { onFilterSelected(EobHistoryPaymentFilter.Paid) },
                label = { Text(EobStrings.t(language, "historyFilterPaid")) }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == EobHistoryPaymentFilter.Pending,
                onClick = { onFilterSelected(EobHistoryPaymentFilter.Pending) },
                label = { Text(EobStrings.t(language, "historyFilterPending")) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HistoryTimelineList(
    language: AppLanguage,
    timelineSections: List<HistoryTimelineSection>,
    listState: LazyListState,
    expandedRecordKey: String,
    selectedRecord: EobRecord?,
    taxVaultFilterState: TaxVaultFilterState,
    showVaultFilterBanner: Boolean,
    onExpandToggle: (EobRecord) -> Unit,
    onDoctorAppealRequested: (EobRecord) -> Unit,
    onAppealInsurance: (EobRecord) -> Unit,
    onDeleteEob: (EobRecord) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        timelineSections.forEach { section ->
            stickyHeader(key = section.lazySectionKey()) {
                HistoryMonthHeader(title = section.header)
            }
            items(
                items = section.rows,
                key = { row -> section.lazyItemKey(row.record) }
            ) { row ->
                HistoryTimelineItemRow(
                    language = language,
                    row = row,
                    isExpanded = expandedRecordKey == row.record.historyListKey(),
                    isSelected = selectedRecord?.matchesHistoryRecord(row.record) == true,
                    taxVaultFilterState = taxVaultFilterState,
                    showVaultFilterBanner = showVaultFilterBanner,
                    onExpandToggle = { onExpandToggle(row.record) },
                    onDoctorAppealRequested = { onDoctorAppealRequested(row.record) },
                    onAppealInsurance = { onAppealInsurance(row.record) },
                    onDeleteEob = { onDeleteEob(row.record) }
                )
            }
        }
    }
}

@Composable
private fun HistoryMonthHeader(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = MaterialTheme.typography.titleSmall.letterSpacing
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTimelineItemRow(
    language: AppLanguage,
    row: HistoryTimelineRow,
    isExpanded: Boolean,
    isSelected: Boolean,
    taxVaultFilterState: TaxVaultFilterState,
    showVaultFilterBanner: Boolean,
    onExpandToggle: () -> Unit,
    onDoctorAppealRequested: () -> Unit,
    onAppealInsurance: () -> Unit,
    onDeleteEob: () -> Unit
) {
    key(row.record.historyListKey()) {
        HistoryTimelineItemRowContent(
            language = language,
            row = row,
            isExpanded = isExpanded,
            isSelected = isSelected,
            taxVaultFilterState = taxVaultFilterState,
            showVaultFilterBanner = showVaultFilterBanner,
            onExpandToggle = onExpandToggle,
            onDoctorAppealRequested = onDoctorAppealRequested,
            onAppealInsurance = onAppealInsurance,
            onDeleteEob = onDeleteEob
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTimelineItemRowContent(
    language: AppLanguage,
    row: HistoryTimelineRow,
    isExpanded: Boolean,
    isSelected: Boolean,
    taxVaultFilterState: TaxVaultFilterState,
    showVaultFilterBanner: Boolean,
    onExpandToggle: () -> Unit,
    onDoctorAppealRequested: () -> Unit,
    onAppealInsurance: () -> Unit,
    onDeleteEob: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()
    var deleteTriggered by remember(row.record.historyListKey()) { mutableStateOf(false) }

    LaunchedEffect(dismissState.currentValue, row.record.historyListKey()) {
        if (!deleteTriggered && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            deleteTriggered = true
            onDeleteEob()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        TimelineNodeCanvas(
            isFirstInMonth = row.isFirstInMonth,
            isLastInMonth = row.isLastInMonth,
            modifier = Modifier
                .width(28.dp)
                .height(if (isExpanded) 300.dp else 132.dp)
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            modifier = Modifier.weight(1f),
            backgroundContent = {
                val direction = dismissState.dismissDirection
                if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color(0xFFE53935),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = EobStrings.t(language, "delete"),
                            tint = Color.White,
                            modifier = Modifier.padding(end = 24.dp)
                        )
                    }
                }
            },
            content = {
                WalletReceiptCard(
                    language = language,
                    record = row.record,
                    isExpanded = isExpanded,
                    isSelected = isSelected,
                    taxVaultFilterState = taxVaultFilterState,
                    showVaultFilterBanner = showVaultFilterBanner,
                    onExpandToggle = onExpandToggle,
                    onDoctorAppealRequested = onDoctorAppealRequested,
                    onAppealInsurance = onAppealInsurance
                )
            }
        )
    }
}

@Composable
private fun TimelineNodeCanvas(
    isFirstInMonth: Boolean,
    isLastInMonth: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val nodeColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val lineWidth = 2.dp.toPx()
        val nodeRadius = 5.dp.toPx()

        if (!isFirstInMonth) {
            drawLine(
                color = lineColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, centerY - nodeRadius),
                strokeWidth = lineWidth
            )
        }
        if (!isLastInMonth) {
            drawLine(
                color = lineColor,
                start = Offset(centerX, centerY + nodeRadius),
                end = Offset(centerX, size.height),
                strokeWidth = lineWidth
            )
        }
        drawCircle(
            color = nodeColor,
            radius = nodeRadius,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = nodeRadius * 0.45f,
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
private fun WalletReceiptCard(
    language: AppLanguage,
    record: EobRecord,
    isExpanded: Boolean,
    isSelected: Boolean,
    taxVaultFilterState: TaxVaultFilterState,
    showVaultFilterBanner: Boolean,
    onExpandToggle: () -> Unit,
    onDoctorAppealRequested: () -> Unit,
    onAppealInsurance: () -> Unit
) {
    val lineCount = record.charges.size.coerceAtLeast(1)
    val elevation = if (isExpanded) 12.dp else 3.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
            .clickable(onClick = onExpandToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.providerName.ifBlank {
                            EobStrings.t(language, "providerNameMissing")
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (isExpanded) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = record.serviceDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = EobStrings.tf(language, "historyReceiptLineCount", lineCount),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (showVaultFilterBanner) {
                val chipLabel = vaultEligibilityLabel(language, record, taxVaultFilterState)
                chipLabel?.let { label ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B7F4B),
                        modifier = Modifier
                            .background(Color(0xFF3DDC84).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = EobStrings.t(language, "patientResponsibility"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = record.totalPatientResponsibility.asCurrency(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (record.totalPatientResponsibility > 0) {
                        Color(0xFFE53935)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                ReceiptDashedDivider()
                Spacer(modifier = Modifier.height(14.dp))
                ReceiptAmountRow(
                    label = EobStrings.t(language, "billed"),
                    amount = record.totalBilledAmount.asCurrency()
                )
                ReceiptAmountRow(
                    label = EobStrings.t(language, "contractualAdjustment"),
                    amount = record.totalContractualAdjustmentAmount.asCurrency()
                )
                ReceiptAmountRow(
                    label = EobStrings.t(language, "paid"),
                    amount = record.totalInsurancePaidAmount.asCurrency()
                )
                ReceiptAmountRow(
                    label = EobStrings.t(language, "patientResponsibility"),
                    amount = record.totalPatientResponsibility.asCurrency(),
                    emphasized = true
                )

                if (isSelected) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HistoryAppealPillButtons(
                        language = language,
                        onDoctorAppealRequested = onDoctorAppealRequested,
                        onAppealInsurance = onAppealInsurance
                    )
                }

                if (record.charges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    ReceiptDashedDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = EobStrings.t(language, "historyProcedureCodes"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    record.charges.forEach { charge ->
                        ReceiptCptLine(language = language, charge = charge)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryAppealPillButtons(
    language: AppLanguage,
    onDoctorAppealRequested: () -> Unit,
    onAppealInsurance: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        HistoryAppealPill(
            label = EobStrings.t(language, "historyAppealDoctorPill"),
            backgroundColor = Color(0xFF2979FF),
            contentDescription = EobStrings.t(language, "historyAppealDoctorPill"),
            onClick = onDoctorAppealRequested
        )
        HistoryAppealPill(
            label = EobStrings.t(language, "historyAppealInsurancePill"),
            backgroundColor = Color(0xFFE53935),
            contentDescription = EobStrings.t(language, "historyAppealInsurancePill"),
            onClick = onAppealInsurance
        )
    }
}

@Composable
private fun HistoryAppealPill(
    label: String,
    backgroundColor: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        modifier = Modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun DoctorAppealStrategyFloater(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onStrategySelected: (DoctorDisputeStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = EobStrings.t(language, "historyDoctorAppealOptionsTitle"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = EobStrings.t(language, "historyDoctorAppealDismiss")
                    )
                }
            }
            DoctorDisputeStrategy.entries.forEach { strategy ->
                Surface(
                    onClick = { onStrategySelected(strategy) },
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF2979FF).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = EobStrings.t(language, strategy.labelKey()),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2979FF)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptCptLine(
    language: AppLanguage,
    charge: EobCharge
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = EobStrings.tf(language, "cptCodeLabel", charge.cptCode),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (charge.cptDescription.isNotBlank()) {
                Text(
                    text = charge.cptDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = charge.billedAmount.asCurrency(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReceiptDashedDivider() {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = dividerColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    }
}

@Composable
private fun ReceiptAmountRow(
    label: String,
    amount: String,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = amount,
            style = if (emphasized) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun vaultEligibilityLabel(
    language: AppLanguage,
    record: EobRecord,
    taxVaultFilterState: TaxVaultFilterState
): String? {
    return when {
        taxVaultFilterState == TaxVaultFilterState.HSA && record.isHsaEligible ->
            EobStrings.t(language, "taxVaultHsaEligibleChip")
        taxVaultFilterState == TaxVaultFilterState.FSA && record.isFsaEligible ->
            EobStrings.t(language, "taxVaultFsaEligibleChip")
        record.isHsaEligible -> EobStrings.t(language, "taxVaultHsaEligibleChip")
        record.isFsaEligible -> EobStrings.t(language, "taxVaultFsaEligibleChip")
        else -> null
    }
}
