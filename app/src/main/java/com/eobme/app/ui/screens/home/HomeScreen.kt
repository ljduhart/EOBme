package com.eobme.app.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eobme.app.EOBmeApp
import com.eobme.app.R
import com.eobme.app.data.local.entity.EobEntity
import com.eobme.app.data.local.entity.InsuranceCardEntity
import com.eobme.app.reference.InsuranceNews
import com.eobme.app.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: EOBmeApp,
    onUploadEob: () -> Unit,
    onEobClick: (Long) -> Unit,
    onCptTracking: () -> Unit,
    onProfile: () -> Unit,
    onSupport: () -> Unit,
    onLogout: () -> Unit
) {
    val userId by app.userPreferences.userId.collectAsState(initial = -1L)
    val eobs by app.eobRepository.observeUserEobs(userId).collectAsState(initial = emptyList())
    val insuranceCard by app.database.insuranceCardDao().observeByUser(userId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.tab_history),
        stringResource(R.string.tab_news),
        stringResource(R.string.tab_cpt)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EOBme", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onCptTracking) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.tab_cpt))
                    }
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profile))
                    }
                    IconButton(onClick = onSupport) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = stringResource(R.string.support))
                    }
                    IconButton(onClick = {
                        scope.launch {
                            app.userPreferences.logout()
                            onLogout()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onUploadEob) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.upload_eob))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            InsuranceCardBanner(insuranceCard)

            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> HistoryTab(eobs = eobs, onEobClick = onEobClick)
                    1 -> NewsTab()
                    2 -> CptSummaryTab(app = app, userId = userId)
                }
            }
        }
    }
}

@Composable
private fun InsuranceCardBanner(card: InsuranceCardEntity?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (card != null && card.imageUri.isNotBlank()) {
                Column {
                    Text(
                        text = card.insuranceName.ifBlank { stringResource(R.string.insurance_card) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (card.subscriberId.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.subscriber_id)}: ${card.subscriberId}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Column {
                    Text(
                        text = if (card?.subscriberId?.isNotBlank() == true)
                            "${stringResource(R.string.subscriber_id)}: ${card.subscriberId}"
                        else
                            stringResource(R.string.no_insurance_card),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.upload_card_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(eobs: List<EobEntity>, onEobClick: (Long) -> Unit) {
    if (eobs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.no_eobs),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.tap_plus_to_upload),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(eobs, key = { it.id }) { eob ->
                EobHistoryCard(eob = eob, onClick = { onEobClick(eob.id) })
            }
        }
    }
}

@Composable
private fun EobHistoryCard(eob: EobEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = eob.providerName.ifBlank { stringResource(R.string.unknown_provider) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = DateUtils.formatDate(eob.dateOfService),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = eob.insuranceName.ifBlank { stringResource(R.string.unknown_insurance) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.billed)}: $${String.format("%.2f", eob.billedAmount)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${stringResource(R.string.paid)}: $${String.format("%.2f", eob.insurancePaid)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun NewsTab() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(InsuranceNews.recentNews) { news ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = news.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = news.summary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = news.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = news.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CptSummaryTab(app: EOBmeApp, userId: Long) {
    val year = DateUtils.currentYear()
    val cptRecords by app.cptRepository.observeByUserAndYear(userId, year).collectAsState(initial = emptyList())
    val grouped = cptRecords.groupBy { it.code }

    val categories = listOf("OV" to stringResource(R.string.office_visits), "LAB" to stringResource(R.string.labs),
        "HOSPITAL" to stringResource(R.string.hospital), "DME" to stringResource(R.string.dme),
        "INJECTION" to stringResource(R.string.injections))

    if (cptRecords.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_cpt_data),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "${stringResource(R.string.cpt_tracking)} - $year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { (catCode, catName) ->
                        val count = cptRecords.count { it.category == catCode }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = catName, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(grouped.entries.toList().sortedBy { it.key }) { (code, records) ->
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
                                text = code,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = records.first().description.ifBlank { stringResource(R.string.no_description) },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "(${records.size}x)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
