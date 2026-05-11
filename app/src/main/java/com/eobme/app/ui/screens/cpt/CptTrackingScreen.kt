package com.eobme.app.ui.screens.cpt

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eobme.app.EOBmeApp
import com.eobme.app.R
import com.eobme.app.reference.CptCodes
import com.eobme.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CptTrackingScreen(
    app: EOBmeApp,
    onBack: () -> Unit
) {
    val userId by app.userPreferences.userId.collectAsState(initial = -1L)
    val years by app.cptRepository.observeYears(userId).collectAsState(initial = emptyList())
    var selectedYear by remember { mutableStateOf(DateUtils.currentYear()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val records by app.cptRepository.observeByUserAndYear(userId, selectedYear).collectAsState(initial = emptyList())

    val filteredRecords = if (selectedCategory != null) {
        records.filter { it.category == selectedCategory }
    } else {
        records
    }
    val grouped = filteredRecords.groupBy { it.code }

    val categoryLabels = mapOf(
        CptCodes.CATEGORY_OV to stringResource(R.string.office_visits),
        CptCodes.CATEGORY_LAB to stringResource(R.string.labs),
        CptCodes.CATEGORY_HOSPITAL to stringResource(R.string.hospital),
        CptCodes.CATEGORY_DME to stringResource(R.string.dme),
        CptCodes.CATEGORY_INJECTION to stringResource(R.string.injections),
        CptCodes.CATEGORY_RADIOLOGY to stringResource(R.string.radiology),
        CptCodes.CATEGORY_SURGERY to stringResource(R.string.surgery),
        CptCodes.CATEGORY_OTHER to stringResource(R.string.other)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cpt_tracking)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (years.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.select_year),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val displayYears = if (years.contains(DateUtils.currentYear())) years else listOf(DateUtils.currentYear()) + years
                        items(displayYears.distinct().sorted()) { year ->
                            FilterChip(
                                selected = year == selectedYear,
                                onClick = { selectedYear = year },
                                label = { Text(year.toString()) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.filter_by_category),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(stringResource(R.string.all)) }
                        )
                    }
                    items(categoryLabels.entries.toList()) { (code, label) ->
                        FilterChip(
                            selected = selectedCategory == code,
                            onClick = { selectedCategory = if (selectedCategory == code) null else code },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(R.string.total_codes)}: ${filteredRecords.size} (${grouped.size} ${stringResource(R.string.unique)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(grouped.entries.toList().sortedByDescending { it.value.size }) { (code, recs) ->
                val info = CptCodes.lookup(code)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${recs.size}x)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = recs.first().category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        val desc = info?.description ?: recs.first().description
                        if (desc.isNotBlank()) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
